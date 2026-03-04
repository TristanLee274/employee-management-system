# 🚀 Antigravity-EMS: Enterprise Microservices Solution

**A high-performance, event-driven Employee Management System engineered to demonstrate Senior-level Java architecture, Clean Code principles, and extreme scalability.**

Built from the ground up as a reference architecture, Antigravity-EMS leverages the cutting-edge capabilities of **Java 21** and **Spring Boot 3.5**. It exemplifies an uncompromising commitment to **SOLID principles**, robust **Design Patterns**, and a rigorous **TDD/BDD testing strategy**. 

---

## ✨ Key Features

### 🏢 Business Capabilities
- **Comprehensive HR Management**: Full lifecycle management of Employees, Departments, and Projects.
- **Dynamic Hierarchy**: Self-referencing data structures for complex departmental trees.
- **Audit Trails**: Immutable tracking of all critical system activities.
- **Asynchronous Notifications**: Real-time email and SMS dispatch based on domain events.

### 🛠️ Technical Excellence
- **Distributed Architecture**: Fully decoupled 8-node Microservices ecosystem.
- **Event-Driven Communication**: Apache Kafka backbone for resilient, asynchronous inter-service messaging.
- **Stateless Security**: Robust RBAC implementation using Spring Security and stateless JWT.
- **Centralized Configuration**: Dynamic configuration management via Spring Cloud Config Server.
- **API Gateway Pattern**: Single entry point with dynamic routing and rate limiting.

---

## 🧰 Tech Stack & Architecture

| Component | Technology | Version / Details |
| :--- | :--- | :--- |
| **☕ Core Language** | Java | 21 (Virtual Threads, Pattern Matching) |
| **🍃 Framework**| Spring Boot | 3.5.11 |
| **🐘 Persistence**| PostgreSQL & Spring Data JPA | Relational data, Flyway Migrations |
| **🍃 NoSQL** | MongoDB | Fast write optimization for Audit & Notifications |
| **📨 Messaging** | Apache Kafka | High-throughput Event Streaming |
| **🛡️ Security** | Spring Security & JWT | Stateless Authentication, Method-Level Security |
| **🚦 Gateway** | Spring Cloud Gateway | API Routing & Circuit Breaking |
| **🗺️ Registry** | Eureka | Service Discovery |
| **🐳 Containerization** | Docker & Compose | Multi-container orchestration |
| **🚀 CI/CD** | Jenkins & SonarQube | Automated Pipeline & Quality Gates |

### Microservices Ecosystem
1. **API Gateway** (`:8080`)
2. **Employee Service** (`:8081`) - Core CRUD & Business Logic
3. **Department Service** (`:8082`) - Organization Trees
4. **Project Service** (`:8083`) - Resource Allocation
5. **Notification Service** (`:8084`) - Event Consumer (MongoDB)
6. **Audit Log Service** (`:8085`) - Event Consumer (MongoDB)
7. **Discovery Server** (`:8761`)
8. **Config Server** (`:8888`)

---

## ⚡ Advanced Java 21 Features

Antigravity-EMS aggressively adopts modern Java features for maximum performance and readability:

- **Virtual Threads (Project Loom)**: Enabled at the Tomcat protocol handler and Kafka listener levels. Transforms the handling of I/O bound tasks, allowing the system to easily process thousands of concurrent requests with a fractional memory footprint compared to platform threads.
- **Records**: Employed pervasively for DTOs (Data Transfer Objects) and internal Domain Events, guaranteeing immutability and dramatically reducing boilerplate.
- **Pattern Matching for `switch`**: Used extensively in Event Processors (e.g., within `@KafkaListener`), enabling highly expressive, polymorphic event handling logic over `sealed` interfaces without fragile `instanceof` checks.
- **Structured Concurrency** *(Preview)*: Utilized for aggregating data from multiple microservices simultaneously, strictly managing task lifecycles to prevent thread leaks upon downstream subtask failures.

---

## 🧬 Design Patterns & SOLID 

The codebase adheres militantly to Clean Architecture and SOLID principles:
- **Single Responsibility (SRP)**: Strict layered architecture (Controller → Service → Repository).
- **Open/Closed (OCP) & Strategy**: Implemented in the `NotificationSender` (Email vs. SMS) and Salary Calculation modules, allowing the introduction of new algorithms without modifying existing code.
- **Observer Pattern**: Fully realized across service boundaries using **Apache Kafka** (e.g., `EmployeeCreatedEvent` triggers downstream operations in Audit and Notification services invisibly to the Publisher).
- **Factory Method**: Used for instantiating complex, variant-based Domain Events seamlessly.
- **Template Method**: Streamlines boilerplate in `AbstractCrudService` while preserving concrete service flexibility via hook methods.

---

## 🧪 Testing Strategy

Quality is built-in from line zero using an uncompromising approach to software validation:

- **Test-Driven Development (TDD)**: Features are developed following rigorous `Red -> Green -> Refactor` cycles.
- **Behavior-Driven Development (BDD)**: Leveraging **Cucumber & Gherkin** to map human-readable business requirements directly to automated end-to-end integration tests.
- **Component Isolation**: Intense use of **JUnit 5** and **Mockito** at the Service layer for pure, high-speed business logic validation.
- **True Integration Tests**: Integration tests utilize **Testcontainers** to spin up actual PostgreSQL and Kafka Docker instances, verifying database queries and inter-service messaging with absolute fidelity.
- **Code Coverage**: Enforced target of **>80%** via JaCoCo policies.

---

## ⚙️ CI/CD Pipeline

The project includes a robust `Jenkinsfile` orchestrating a comprehensive DevOps lifecycle:
1. **SCM Checkout** → 2. **Maven Build** → 3. **Unit Tests** → 4. **Integration Tests** (Testcontainers) → 5. **SonarQube Static Analysis**   
*<Quality Gate strictly enforces 0 Bugs, 0 Vulnerabilities, Grade A Maintainability>*  
→ 6. **Docker Image Build & Push** → 7. **Kubernetes Dev Deployment**.

---

## 🚀 Getting Started

Follow these steps to boot the entire microservices cluster locally:

### Prerequisites
- JDK 21
- Maven 3.9+
- Docker & docker-compose

### Run the Infrastructure
Spin up PostgreSQL, MongoDB, Kafka, and Zookeeper:
```bash
docker-compose -f docker-compose.infra.yml up -d
```

### Build & Run the Services
```bash
# Build all modules
mvn clean package -DskipTests

# Start services (via IDE or java -jar)
java -jar ems-discovery-server/target/ems-discovery-server.jar
java -jar ems-config-server/target/ems-config-server.jar
# (Wait for Discovery & Config to start, then launch remaining services)
java -jar ems-gateway/target/ems-gateway.jar
java -jar ems-employee-service/target/ems-employee-service.jar
# ...
```

*API Documentation (Swagger UI) is available at: `http://localhost:8080/swagger-ui.html`*

---
