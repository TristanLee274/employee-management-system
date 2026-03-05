# ╔══════════════════════════════════════════════════════════════════════════════╗
# ║  Antigravity-EMS — Multi-Stage Docker Build                                ║
# ║  Dockerfile for Spring Boot 3.5.11 / Java 21                               ║
# ╚══════════════════════════════════════════════════════════════════════════════╝
#
# ═══════════════════════════════════════════════════════════════════════════════
#  DOCKER MULTI-STAGE BUILD — WHY TWO STAGES?
# ═══════════════════════════════════════════════════════════════════════════════
#
#  A multi-stage build uses MULTIPLE FROM instructions. Each FROM starts a new
#  build stage. Only the FINAL stage becomes the shipped Docker image. Previous
#  stages are discarded after we COPY artifacts from them.
#
#  WHY THIS MATTERS:
#
#  Single-stage build (BAD):
#    ┌──────────────────────────────────────────────────────────────┐
#    │  Final Image: ~800MB                                        │
#    │  Contains: JDK (compiler + tools) + Maven + .m2 cache       │
#    │            + source code + test code + build artifacts       │
#    │  Security: Compiler and build tools are attack surface       │
#    │  Startup: Larger image → slower pull → slower pod startup    │
#    └──────────────────────────────────────────────────────────────┘
#
#  Multi-stage build (THIS FILE):
#    ┌──────────────────────────────────────────────────────────────┐
#    │  Stage 1 (builder): JDK + Maven → compiles and packages JAR │
#    │  Stage 2 (runner):  JRE-only + JAR → runs the application   │
#    │                                                              │
#    │  Final Image: ~200MB (JRE Alpine + thin JAR)                │
#    │  Contains: ONLY what's needed to RUN (no compiler, no Maven)│
#    │  Security: Minimal attack surface (no build tools)           │
#    │  Startup: Smaller image → faster pull → faster pod startup   │
#    └──────────────────────────────────────────────────────────────┘
#
#  "BUILD ONCE, RUN ANYWHERE" PHILOSOPHY:
#    The JAR built inside Docker is the SAME JAR that runs in:
#    • Developer's laptop (docker run ...)
#    • Jenkins CI pipeline (docker build + docker push)
#    • Kubernetes staging cluster (kubectl apply)
#    • Kubernetes production cluster (same image, different config)
#
#    This eliminates the classic "It works on my machine" problem.
#    The Docker image IS the deployment unit — identical bytes everywhere.
#
#  IMAGE SIZE COMPARISON:
#    ┌─────────────────────────────┬────────────┐
#    │  Approach                   │  Size      │
#    ├─────────────────────────────┼────────────┤
#    │  eclipse-temurin:21-jdk     │  ~460MB    │
#    │  + Maven + source + deps   │  +350MB    │
#    │  = Single-stage total      │  ~810MB    │
#    ├─────────────────────────────┼────────────┤
#    │  eclipse-temurin:21-jre-alpine│  ~120MB  │
#    │  + Application JAR          │  +80MB     │
#    │  = Multi-stage total       │  ~200MB    │
#    └─────────────────────────────┴────────────┘
#    → 75% size reduction with multi-stage build!


# ═══════════════════════════════════════════════════════════════════════════════
#  STAGE 1: BUILDER — Compile and Package the Application
# ═══════════════════════════════════════════════════════════════════════════════
#
#  This stage uses the FULL JDK (not JRE) because we need:
#  • javac compiler for compiling .java → .class files
#  • Maven for dependency resolution and build lifecycle
#
#  The "AS builder" alias lets us reference this stage later with
#  COPY --from=builder to extract the compiled JAR.
#
#  eclipse-temurin: the official Eclipse Adoptium JDK distribution.
#  Preferred over deprecated AdoptOpenJDK for enterprise use.
#
FROM eclipse-temurin:21-jdk AS builder

# Set the working directory inside the container
# All subsequent commands run relative to /app
WORKDIR /app

# ── Layer Caching Strategy ──────────────────────────────────────────────────
# Docker builds images layer-by-layer. Each instruction creates a layer.
# If a layer hasn't changed, Docker reuses it from cache → MUCH faster builds.
#
# STRATEGY: Copy dependency-resolution files FIRST, then source code.
# Maven downloads dependencies based on pom.xml. If pom.xml hasn't changed,
# Docker reuses the cached dependency layer (the slowest part of the build).
# Only when source code changes does Docker need to recompile.

# Step 1: Copy Maven wrapper and pom.xml (changes rarely)
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Step 2: Download all dependencies (cached if pom.xml unchanged)
# go-offline: downloads ALL required dependencies without compiling
# This creates a cached layer that's reused on every build unless
# pom.xml changes (which is rare during active development).
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B -q

# Step 3: Copy source code (changes frequently → only this layer rebuilds)
COPY src/ src/

# Step 4: Build the fat JAR, skipping tests (already tested in CI pipeline)
# -DskipTests: Tests are run in Jenkins Stage 3 (Unit Test).
#   Running them again here would be redundant and slow.
#   The CI pipeline guarantees that only tested code reaches this stage.
# -Dspring-boot.repackage.skip=false: ensure Spring Boot fat JAR is created
RUN ./mvnw package -DskipTests -B -q


# ═══════════════════════════════════════════════════════════════════════════════
#  STAGE 2: RUNNER — Minimal Runtime Image
# ═══════════════════════════════════════════════════════════════════════════════
#
#  This stage uses JRE (Java Runtime Environment) instead of JDK:
#  • JRE = Java classes + JVM = everything needed to RUN Java applications
#  • JDK = JRE + compiler + development tools = NOT needed in production
#
#  Alpine Linux: minimal Linux distribution (~5MB base) optimized for containers.
#  Combined with JRE, this gives us the smallest possible image.
#
#  SECURITY BENEFIT: Fewer installed packages → fewer potential CVEs (vulnerabilities).
#  A JDK image has ~400 packages; JRE Alpine has ~50. Each package is a potential
#  attack vector that security scanners (Trivy, Snyk) flag.
#
FROM eclipse-temurin:21-jre-alpine AS runner

# ── Security: Run as Non-Root User ──────────────────────────────────────────
# By default, Docker containers run as root. This is a SECURITY RISK:
# if an attacker exploits a vulnerability in the app, they get root access
# to the container (and potentially the host via container escape).
#
# Creating a dedicated "appuser" with no login shell and no home directory
# follows the Principle of Least Privilege — the app process can only
# access what it needs, nothing more.
#
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup -s /bin/false

WORKDIR /app

# ── Copy the JAR from the Builder Stage ─────────────────────────────────────
# COPY --from=builder: This is the key multi-stage instruction.
# It copies ONLY the compiled JAR from the builder stage, leaving behind:
# • 460MB JDK
# • 200MB Maven cache (.m2)
# • All source code and test code
# • Build tools and intermediate files
#
# The JAR file is the ONLY artifact that crosses the stage boundary.
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to the non-root user
RUN chown appuser:appgroup app.jar

# Switch to non-root user for all subsequent commands and runtime
USER appuser

# ── Expose Application Port ─────────────────────────────────────────────────
# EXPOSE is documentation — it tells Docker (and humans reading this file)
# which port the application listens on. It does NOT publish the port;
# that's done at runtime with `docker run -p 8080:8080`.
EXPOSE 8080

# ── Health Check ────────────────────────────────────────────────────────────
# Docker's built-in health check mechanism, using Spring Actuator's health
# endpoint. Docker polls this every 30 seconds:
# • If /api/actuator/health returns HTTP 200 → container is HEALTHY
# • If it fails 3 times in a row → container is UNHEALTHY → Docker/K8s
#   can automatically restart it
#
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/actuator/health || exit 1

# ── JVM Configuration for Containers ────────────────────────────────────────
# ENTRYPOINT defines the command that runs when the container starts.
# Using exec form (JSON array) ensures the JVM receives SIGTERM properly
# for graceful shutdown — critical for Kubernetes pod termination.
#
# JVM flags explained:
#   -XX:+UseContainerSupport
#     → JVM detects container memory/CPU limits (not the host's).
#       Without this, a container with 512MB limit might try to use 8GB
#       of heap (the host's memory), causing OOM kills.
#
#   -XX:MaxRAMPercentage=75.0
#     → Use 75% of the container's memory limit for heap.
#       Leaves 25% for JVM metaspace, thread stacks, native memory,
#       and the OS. In a 512MB container: heap = 384MB, other = 128MB.
#
#   -Djava.security.egd=file:/dev/urandom
#     → Use non-blocking random number generator for faster startup.
#       The default /dev/random can block in containers with low entropy,
#       causing 30+ second delays during Spring Boot initialization
#       (especially for JWT key generation and session IDs).
#
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/urandom", \
    "-jar", "app.jar"]
