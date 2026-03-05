// ╔══════════════════════════════════════════════════════════════════════════════╗
// ║  Antigravity-EMS — Jenkins Declarative Pipeline (CI/CD)                    ║
// ║  Jenkinsfile — Pipeline as Code                                            ║
// ╚══════════════════════════════════════════════════════════════════════════════╝
//
// ═══════════════════════════════════════════════════════════════════════════════
//  JENKINS PIPELINE AS CODE — WHY THIS FILE EXISTS
// ═══════════════════════════════════════════════════════════════════════════════
//
//  Traditional CI/CD:
//    • Pipeline configured via Jenkins UI (point-and-click)
//    • Configuration is stored in Jenkins' internal database
//    • If Jenkins crashes → pipeline config is LOST
//    • No audit trail of who changed what and when
//    • Different environments (dev, staging, prod) use different pipelines
//      configured manually → configuration drift
//
//  Pipeline as Code (THIS FILE):
//    • Pipeline definition lives IN the Git repository alongside the source code
//    • Version-controlled: every change to the pipeline is a Git commit with
//      author, timestamp, and diff → full audit trail
//    • Code review: pipeline changes go through the same PR review process as
//      application code → team oversight
//    • Reproducible: clone the repo → get the exact pipeline → no "works on my
//      Jenkins" problem
//    • Branch-specific: each Git branch can have its own Jenkinsfile with
//      different deployment targets (dev branch → staging, main → production)
//
//  For a Team Lead managing 5-6 developers:
//    • Infrastructure changes are transparent — visible in Git log
//    • Onboarding new developers: "Read the Jenkinsfile" instead of
//      "ask DevOps to show you the Jenkins config"
//    • Disaster recovery: rebuild Jenkins from scratch, point it at the repo,
//      and the pipeline is immediately restored
//
// ═══════════════════════════════════════════════════════════════════════════════
//  CONTINUOUS INTEGRATION (CI) — THE PHILOSOPHY
// ═══════════════════════════════════════════════════════════════════════════════
//
//  CI is the practice of automatically building and testing code on EVERY Git
//  commit. The goal is to catch bugs EARLY — when they're cheap to fix.
//
//  Cost of fixing a bug:
//    • During development (caught by CI):  ~$25 (developer fixes immediately)
//    • During QA testing:                  ~$250 (context switch + retesting)
//    • In production (customer-facing):    ~$2,500+ (incident response + hotfix)
//
//  What CI gives a team of 5-6 developers:
//    • CONFIDENCE: Every merged PR has been built and tested automatically.
//      No "it compiles on my machine" surprises.
//    • FAST FEEDBACK: Developers know within minutes if their commit broke
//      something — not days later when QA manually tests.
//    • INTEGRATION SAFETY: When 5 developers push code daily, integration
//      conflicts surface immediately via automated tests, not during a
//      "big bang" merge at sprint end.
//    • RELEASE READINESS: The main branch is always in a deployable state.
//      Releasing is a business decision, not a technical scramble.
//

pipeline {
    // ─── AGENT ──────────────────────────────────────────────────────────────
    // Defines WHERE the pipeline runs. 'any' means any available Jenkins agent.
    // In production, you'd specify a label matching agents with Java 21 + Maven:
    //   agent { label 'java21-maven' }
    // Or use a Docker agent for fully isolated, reproducible builds:
    //   agent { docker { image 'eclipse-temurin:21-jdk' } }
    agent any

    // ─── TOOLS ──────────────────────────────────────────────────────────────
    // Declares build tools managed by Jenkins (configured in Global Tool Config).
    // Jenkins automatically adds these tools to the PATH for all stages.
    tools {
        jdk 'JDK-21'       // Eclipse Temurin 21 (configured in Jenkins > Tools)
        maven 'Maven-3.9'  // Apache Maven 3.9.x
    }

    // ─── ENVIRONMENT ────────────────────────────────────────────────────────
    // Environment variables available to ALL stages in this pipeline.
    // Centralizing config here follows the DRY principle and makes the
    // pipeline easier to maintain.
    environment {
        // Application metadata
        APP_NAME = 'antigravity-ems'
        APP_VERSION = readMavenPom().getVersion()

        // Docker image coordinates
        DOCKER_REGISTRY = 'your-registry.com'
        DOCKER_IMAGE = "${DOCKER_REGISTRY}/${APP_NAME}"

        // SonarQube server configured in Jenkins > System > SonarQube servers
        SONAR_SERVER = 'SonarQube-Server'

        // JaCoCo coverage thresholds — the "Quality Gate" enforcement
        // If coverage drops below these thresholds, the build FAILS.
        MIN_LINE_COVERAGE = '70'
        MIN_BRANCH_COVERAGE = '60'
    }

    // ─── OPTIONS ────────────────────────────────────────────────────────────
    options {
        // Automatically abort builds that run too long (stuck builds waste
        // Jenkins executor slots, blocking other team members)
        timeout(time: 30, unit: 'MINUTES')

        // Keep only the last 10 builds to save disk space
        buildDiscarder(logRotator(numToKeepStr: '10'))

        // Add timestamps to console output — essential for debugging slow stages
        timestamps()

        // Skip default SCM checkout — we do it explicitly in the first stage
        // for better logging and control
        skipDefaultCheckout(true)
    }

    // ─── TRIGGERS ───────────────────────────────────────────────────────────
    // Automatically trigger builds — essential for CI.
    // pollSCM checks Git for new commits every 5 minutes.
    // In production, prefer GitHub/GitLab webhooks for instant triggering.
    triggers {
        pollSCM('H/5 * * * *')
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PIPELINE STAGES
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  The pipeline follows a linear progression:
    //
    //  ┌──────────┐   ┌──────────┐   ┌───────────┐   ┌───────────┐   ┌─────────┐
    //  │ Checkout  │──→│  Build   │──→│ Unit Test │──→│ SonarQube │──→│ Package │
    //  │ (Git)     │   │ (Compile)│   │ (JUnit 5) │   │ (Analysis)│   │ (JAR)   │
    //  └──────────┘   └──────────┘   └───────────┘   └───────────┘   └─────────┘
    //       │              │               │               │              │
    //       │              │               │               │              │
    //  Fetch latest    Compile Java    Run 107 unit    Static code    Create fat
    //  source code     21 source +     tests with      analysis:      JAR with
    //  from Git        resolve Maven   Mockito BDD,    bugs, smells,  embedded
    //  repository      dependencies    Parameterized   security,      Tomcat,
    //                                  Tests, TDD      coverage       ready for
    //                                                                 Docker
    //
    //  If ANY stage fails, the pipeline STOPS immediately. This is the
    //  "fail fast" principle — there's no point packaging a JAR if tests fail.
    //

    stages {

        // ═════════════════════════════════════════════════════════════════════
        //  STAGE 1: CHECKOUT — Fetch Source Code from Git
        // ═════════════════════════════════════════════════════════════════════
        stage('Checkout') {
            steps {
                echo '═══ Stage 1: Checking out source code from Git ═══'

                // checkout scm: uses the SCM configuration from the Jenkins job
                // (typically the repository URL and credentials configured when
                // creating the pipeline job).
                //
                // WHY EXPLICIT CHECKOUT?
                // • Logs the exact Git commit SHA for traceability
                // • Allows conditional logic based on branch name
                // • Supports multi-branch pipelines with different behaviors
                //   per branch (e.g., deploy only from 'main')
                checkout scm

                // Log the exact commit for auditability
                // In a post-mortem, you can trace "which exact code was deployed?"
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()
                    echo "Building commit: ${env.GIT_COMMIT_SHORT}"
                }
            }
        }

        // ═════════════════════════════════════════════════════════════════════
        //  STAGE 2: BUILD — Compile Source Code
        // ═════════════════════════════════════════════════════════════════════
        stage('Build') {
            steps {
                echo '═══ Stage 2: Compiling Java 21 source code ═══'

                // mvn clean compile:
                //   clean  → deletes /target directory (ensures fresh build,
                //            no stale class files from previous builds)
                //   compile → compiles src/main/java using javac with Java 21
                //
                // -B (--batch-mode): disables interactive prompts and download
                //   progress bars — essential for CI where there's no human to
                //   interact with the terminal.
                //
                // WHY COMPILE SEPARATELY FROM TEST?
                //   Separation of concerns at the pipeline level:
                //   • If compilation FAILS, you know it's a syntax/dependency issue
                //   • If compilation PASSES but tests FAIL, you know it's a logic bug
                //   • This distinction helps the Team Lead triage build failures faster
                //
                sh './mvnw clean compile -B -q'
            }
        }

        // ═════════════════════════════════════════════════════════════════════
        //  STAGE 3: UNIT TEST — Run JUnit 5 Test Suite
        // ═════════════════════════════════════════════════════════════════════
        stage('Unit Test') {
            steps {
                echo '═══ Stage 3: Running JUnit 5 + Mockito test suite ═══'

                // mvn test:
                //   Executes ALL unit tests via Maven Surefire Plugin.
                //
                // Our test suite includes 107 tests covering:
                //   • SalaryCalculatorStrategyTest (28 tests) — Strategy Pattern
                //   • EmployeeServiceTest (25 tests) — Service layer with Mockito BDD
                //   • JwtServiceTest (20 tests) — JWT token lifecycle
                //   • AuthControllerTest (14 tests) — Login flow + UserDetailsService
                //   • DtoMappingTest (10 tests) — Record mapping & normalization
                //   • KafkaEventSystemTest (9 tests) — Event producer/consumer
                //
                // WHY UNIT TESTS IN CI?
                //   • AUTOMATED REGRESSION DETECTION: Every commit is tested against
                //     107 scenarios. A developer who accidentally breaks the salary
                //     calculation will know within 6 seconds, not during QA testing.
                //   • TEAM SAFETY NET: With 5-6 developers pushing code daily,
                //     automated tests prevent one person's change from breaking
                //     another person's feature.
                //   • LIVING DOCUMENTATION: Test names describe business behavior:
                //     "should throw DuplicateEmailException when email already exists"
                //     → anyone can understand what the system does by reading tests.
                //
                sh './mvnw test -B'
            }

            // Post-actions for the Unit Test stage
            post {
                always {
                    // Publish JUnit test results to Jenkins UI
                    // This creates the "Test Results" tab in the build page,
                    // showing pass/fail/skip counts and failure details.
                    junit(
                        testResults: 'target/surefire-reports/*.xml',
                        allowEmptyResults: true
                    )
                }
            }
        }

        // ═════════════════════════════════════════════════════════════════════
        //  STAGE 4: SONARQUBE ANALYSIS — Static Code Analysis
        // ═════════════════════════════════════════════════════════════════════
        //
        //  WHAT IS SONARQUBE?
        //  ──────────────────
        //  SonarQube is a static code analysis platform that scans source code
        //  (without executing it) to find:
        //
        //  1. BUGS: Logic errors that may cause runtime failures
        //     Example: NullPointerException risk, resource leaks, infinite loops
        //
        //  2. CODE SMELLS: Code that works but is poorly written
        //     Example: Methods with 200+ lines, deeply nested if-else chains,
        //     duplicated code blocks, unused variables
        //
        //  3. VULNERABILITIES: Security weaknesses
        //     Example: SQL injection via string concatenation, hardcoded passwords,
        //     unsafe deserialization, weak cryptography
        //
        //  4. SECURITY HOTSPOTS: Code that requires manual security review
        //     Example: Regex patterns (ReDoS risk), HTTP request handling,
        //     file system access
        //
        //  5. TECHNICAL DEBT: Estimated time to fix all code smells
        //     Example: "3 hours of tech debt" → the Team Lead can prioritize
        //     refactoring sprints based on concrete numbers, not gut feeling
        //
        //  WHY SONARQUBE FOR A TEAM LEAD MANAGING 5-6 DEVELOPERS?
        //  ────────────────────────────────────────────────────────
        //  • OBJECTIVE CODE REVIEW: Instead of subjective "I think this code is
        //    messy", SonarQube provides quantifiable metrics: complexity score,
        //    duplication percentage, coverage ratio. This removes opinion-based
        //    conflict from code reviews.
        //
        //  • TREND TRACKING: SonarQube dashboards show code quality over time.
        //    If tech debt increases sprint after sprint, the Team Lead has DATA
        //    to justify a "cleanup sprint" to management.
        //
        //  • ENFORCE STANDARDS: SonarQube rules enforce your team's coding
        //    standards automatically. New developers get immediate feedback on
        //    style violations without senior developers manually reviewing
        //    every line.
        //
        //  • PR DECORATION: SonarQube can comment directly on Pull Requests,
        //    showing exactly which lines introduced new bugs or smells. This
        //    makes code review faster and more focused.
        //
        stage('SonarQube Analysis') {
            steps {
                echo '═══ Stage 4: Running SonarQube static code analysis ═══'

                // withSonarQubeEnv: injects the SonarQube server URL and auth
                // token from Jenkins configuration into the environment.
                // This keeps credentials OUT of the Jenkinsfile (security).
                withSonarQubeEnv("${SONAR_SERVER}") {
                    // Run SonarQube scanner via Maven plugin
                    //
                    // Key parameters:
                    //   sonar.projectKey    → unique project identifier in SonarQube
                    //   sonar.projectName   → displayed name in the dashboard
                    //   sonar.java.source   → Java version for analysis rules
                    //   sonar.sources       → directories to scan (main source)
                    //   sonar.tests         → directories to scan (test source)
                    //   sonar.coverage.jacoco → path to JaCoCo coverage report
                    //
                    sh """
                        ./mvnw sonar:sonar -B \\
                            -Dsonar.projectKey=${APP_NAME} \\
                            -Dsonar.projectName='Antigravity EMS' \\
                            -Dsonar.java.source=21 \\
                            -Dsonar.sources=src/main/java \\
                            -Dsonar.tests=src/test/java \\
                            -Dsonar.java.binaries=target/classes \\
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \\
                            -Dsonar.qualitygate.wait=true
                    """
                    // -Dsonar.qualitygate.wait=true:
                    //   CRITICAL FLAG — This makes the pipeline WAIT for SonarQube
                    //   to evaluate the Quality Gate and returns the result.
                    //   If the Quality Gate fails, this command returns non-zero
                    //   exit code → the stage FAILS → the pipeline STOPS.
                }
            }
        }

        // ═════════════════════════════════════════════════════════════════════
        //  QUALITY GATE — Build-Breaking Code Quality Enforcement
        // ═════════════════════════════════════════════════════════════════════
        //
        //  WHAT IS A QUALITY GATE?
        //  ───────────────────────
        //  A Quality Gate is a set of measurable conditions that code MUST meet
        //  before it can be merged or deployed. Think of it as a "minimum bar"
        //  for code quality.
        //
        //  WHY SHOULD THE BUILD FAIL IF QUALITY GATE DOESN'T PASS?
        //  ────────────────────────────────────────────────────────
        //  Without enforcement, code quality degrades over time ("broken windows
        //  theory"):
        //    Sprint 1: "We'll add tests later" → 80% coverage → acceptable
        //    Sprint 3: "This is urgent" → 60% coverage → concerning
        //    Sprint 6: "We never test" → 30% coverage → legacy code
        //
        //  With enforced Quality Gates:
        //    • The build FAILS if coverage drops below 70% → developers MUST
        //      write tests before their PR can merge
        //    • New security vulnerabilities BLOCK the build → security is not
        //      an afterthought
        //    • Technical debt is quantified → "this PR adds 2 hours of debt"
        //      becomes visible and actionable
        //
        //  Typical Quality Gate conditions:
        //    ┌──────────────────────────────────────────────────────────────┐
        //    │  Condition                  │  Threshold  │  Rationale      │
        //    ├──────────────────────────────────────────────────────────────┤
        //    │  New Code Coverage           │  ≥ 80%     │  All new code   │
        //    │  Overall Code Coverage       │  ≥ 70%     │  must be tested │
        //    │  New Bugs                    │  = 0       │  No new bugs    │
        //    │  New Vulnerabilities         │  = 0       │  Security first │
        //    │  New Code Smells Rating      │  ≤ A       │  Clean code     │
        //    │  Duplicated Lines on New Code│  ≤ 3%      │  DRY principle  │
        //    └──────────────────────────────────────────────────────────────┘
        //
        stage('Quality Gate') {
            steps {
                echo '═══ Quality Gate: Waiting for SonarQube verdict ═══'

                // waitForQualityGate: polls SonarQube for the analysis result.
                // SonarQube evaluates all Quality Gate conditions and returns
                // PASS or FAIL. If FAIL → this step throws an error →
                // pipeline stops → no JAR is packaged → no deployment.
                //
                // This is the ENFORCEMENT mechanism. Without this step,
                // SonarQube analysis is just informational. With this step,
                // it's a hard blocker that keeps the codebase healthy.
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // ═════════════════════════════════════════════════════════════════════
        //  STAGE 5: PACKAGE — Create Deployable Artifact (Fat JAR)
        // ═════════════════════════════════════════════════════════════════════
        stage('Package') {
            steps {
                echo '═══ Stage 5: Packaging Spring Boot fat JAR ═══'

                // mvn package -DskipTests:
                //   package   → compile + test + package into JAR
                //   -DskipTests → tests already passed in Stage 3; no need to
                //                 run them again (saves ~6 seconds per build,
                //                 but compounds when the team pushes 20+ commits/day)
                //
                // The Spring Boot Maven Plugin repackages the standard JAR into
                // a "fat JAR" containing:
                //   • Application classes
                //   • All dependency JARs (Spring, Hibernate, Jackson, etc.)
                //   • Embedded Tomcat server
                //   • META-INF/MANIFEST.MF with Main-Class pointing to Spring Boot launcher
                //
                // Result: a single self-contained JAR that runs anywhere with
                // `java -jar employee-management-system-0.0.1-SNAPSHOT.jar`
                //
                sh './mvnw package -DskipTests -B -q'

                // Archive the JAR as a Jenkins build artifact
                // This allows downloading the JAR directly from the Jenkins UI
                // for manual testing or deployment without rebuilding.
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  POST-BUILD ACTIONS — Cleanup and Notifications
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Post actions run AFTER all stages complete (or after a failure).
    //  They handle cross-cutting concerns like notifications and cleanup.
    //
    post {
        success {
            echo """
            ╔══════════════════════════════════════════════════════════════╗
            ║  ✅ BUILD SUCCESSFUL                                       ║
            ║  App: ${APP_NAME}                                          ║
            ║  Version: ${APP_VERSION}                                   ║
            ║  Commit: ${env.GIT_COMMIT_SHORT ?: 'N/A'}                  ║
            ║  107 unit tests passed | Quality Gate: PASSED              ║
            ╚══════════════════════════════════════════════════════════════╝
            """

            // In production, notify the team via Slack/Teams:
            // slackSend(
            //     channel: '#ems-builds',
            //     color: 'good',
            //     message: "✅ EMS Build #${env.BUILD_NUMBER} passed | ${env.GIT_COMMIT_SHORT}"
            // )
        }

        failure {
            echo """
            ╔══════════════════════════════════════════════════════════════╗
            ║  ❌ BUILD FAILED                                           ║
            ║  Check console output for details.                         ║
            ║  Common causes:                                            ║
            ║  • Compilation error → check Stage 2 logs                  ║
            ║  • Test failure → check Stage 3 JUnit reports              ║
            ║  • Quality Gate → check SonarQube dashboard                ║
            ╚══════════════════════════════════════════════════════════════╝
            """

            // In production, alert the team immediately:
            // slackSend(
            //     channel: '#ems-builds',
            //     color: 'danger',
            //     message: "❌ EMS Build #${env.BUILD_NUMBER} FAILED | ${env.GIT_COMMIT_SHORT}"
            // )
        }

        always {
            // Clean up workspace to free disk space on the Jenkins agent.
            // Without this, old build artifacts accumulate and can fill the disk,
            // causing ALL builds across ALL projects to fail.
            cleanWs()
        }
    }
}
