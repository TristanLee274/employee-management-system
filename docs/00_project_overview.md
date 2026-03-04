# 🏗️ Antigravity-EMS — Project Overview

## 1. Introduction

**Antigravity-EMS** is a reference project designed for training and evaluating **Senior Java Developer** candidates. The Employee Management System (EMS) is built using a **Microservices Architecture**, strictly adhering to modern software engineering principles.

---

## 2. High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            CLIENT LAYER                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                      │
│  │  Web Browser  │  │  Mobile App  │  │  API Client  │                      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘                      │
└─────────┼─────────────────┼─────────────────┼───────────────────────────────┘
          │                 │                 │
          ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       API GATEWAY LAYER                                     │
│  ┌─────────────────────────────────────────────────────────────────┐        │
│  │              Spring Cloud Gateway (Port 8080)                   │        │
│  │  • Rate Limiting  • Load Balancing  • JWT Validation            │        │
│  │  • Request Routing  • Circuit Breaker                           │        │
│  └─────────────────────────────────────────────────────────────────┘        │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          │                       │                       │
          ▼                       ▼                       ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐
│  EMPLOYEE SVC   │  │  DEPARTMENT SVC │  │   PROJECT SVC       │
│  (Port 8081)    │  │  (Port 8082)    │  │   (Port 8083)       │
│                 │  │                 │  │                     │
│ • CRUD Employee │  │ • CRUD Dept     │  │ • CRUD Project      │
│ • Search/Filter │  │ • Dept Tree     │  │ • Assignment        │
│ • Profile Mgmt  │  │ • Statistics    │  │ • Timeline          │
│                 │  │                 │  │                     │
│  ┌───────────┐  │  │  ┌───────────┐  │  │  ┌───────────────┐  │
│  │PostgreSQL │  │  │  │PostgreSQL │  │  │  │  PostgreSQL   │  │
│  │  (emp_db) │  │  │  │ (dept_db) │  │  │  │  (proj_db)   │  │
│  └───────────┘  │  │  └───────────┘  │  │  └───────────────┘  │
└────────┬────────┘  └────────┬────────┘  └──────────┬──────────┘
         │                    │                      │
         ▼                    ▼                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     MESSAGING LAYER (Apache Kafka)                          │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │ employee-events  │  │ department-events│  │ project-events   │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
│  ┌──────────────────┐  ┌──────────────────┐                                │
│  │ notification-topic│  │ audit-log-topic  │                                │
│  └──────────────────┘  └──────────────────┘                                │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────┐
          ▼                                           ▼
┌─────────────────────┐                  ┌─────────────────────┐
│ NOTIFICATION SVC    │                  │  AUDIT LOG SVC      │
│ (Port 8084)         │                  │  (Port 8085)        │
│                     │                  │                     │
│ • Email / SMS       │                  │ • Activity Logs     │
│ • Push Notif        │                  │ • Change Tracking   │
│ • Event Processing  │                  │ • Report Gen        │
│                     │                  │                     │
│ ┌─────────────────┐ │                  │ ┌─────────────────┐ │
│ │   MongoDB       │ │                  │ │   MongoDB       │ │
│ │ (notif_db)      │ │                  │ │ (audit_db)      │ │
│ └─────────────────┘ │                  │ └─────────────────┘ │
└─────────────────────┘                  └─────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE LAYER                                     │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────────────┐        │
│  │ Spring Cloud   │  │  Eureka /      │  │ Centralized Logging    │        │
│  │ Config Server  │  │  Consul        │  │ (ELK Stack)            │        │
│  │ (Port 8888)    │  │  (Port 8761)   │  │                        │        │
│  └────────────────┘  └────────────────┘  └────────────────────────┘        │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Tech Stack

| Component             | Technology                             | Version     |
|-----------------------|----------------------------------------|-------------|
| **Language**          | Java (Virtual Threads, Records)        | 21          |
| **Framework**         | Spring Boot                            | 3.5.11      |
| **Build Tool**        | Maven (JAR packaging)                  | 3.9+        |
| **Relational DB**     | PostgreSQL                             | 16+         |
| **NoSQL DB**          | MongoDB (logs, notifications)          | 7+          |
| **Messaging**         | Apache Kafka                           | 3.7+        |
| **Security**          | Spring Security 6.x + JWT (stateless) | 6.4+        |
| **API Gateway**       | Spring Cloud Gateway                   | 4.2+        |
| **Service Discovery** | Spring Cloud Eureka                    | 4.2+        |
| **Config Server**     | Spring Cloud Config                    | 4.2+        |
| **API Docs**          | Swagger / OpenAPI 3 (springdoc)        | 2.8+        |
| **Testing**           | JUnit 5, Mockito, Cucumber             | —           |
| **CI/CD**             | Jenkins, SonarQube                     | —           |
| **Containerization**  | Docker, Docker Compose                 | —           |

---

## 4. Microservices Catalog

| #  | Service              | Port  | Database   | Description                        |
|----|----------------------|-------|------------|------------------------------------|
| 1  | API Gateway          | 8080  | —          | Routing, Rate Limiting, JWT Auth   |
| 2  | Employee Service     | 8081  | PostgreSQL | Employee lifecycle management      |
| 3  | Department Service   | 8082  | PostgreSQL | Department hierarchy management    |
| 4  | Project Service      | 8083  | PostgreSQL | Project planning & assignment      |
| 5  | Notification Service | 8084  | MongoDB    | Messaging dispatcher (Email, SMS)  |
| 6  | Audit Log Service    | 8085  | MongoDB    | Activity logging & audit trails    |
| 7  | Config Server        | 8888  | Git repo   | Centralized external configuration |
| 8  | Discovery Server     | 8761  | —          | Service registry (Eureka)          |

---

## 5. Module Structure (Maven Multi-Module)

```
antigravity-ems/
├── pom.xml                          # Parent POM
├── ems-common/                      # Shared DTOs, Utils, Exceptions
│   └── pom.xml
├── ems-gateway/                     # Spring Cloud Gateway
│   └── pom.xml
├── ems-employee-service/            # Employee microservice
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/ems/employee/
│       │   ├── controller/
│       │   ├── service/
│       │   ├── repository/
│       │   ├── model/
│       │   ├── dto/
│       │   ├── mapper/
│       │   ├── config/
│       │   ├── exception/
│       │   └── event/
│       └── test/
├── ems-department-service/          # Department microservice
├── ems-project-service/             # Project microservice
├── ems-notification-service/        # Notification (MongoDB)
├── ems-audit-service/               # Audit log (MongoDB)
├── ems-config-server/               # Centralized config
├── ems-discovery-server/            # Eureka server
└── docs/                           # Documentation
    ├── 00_project_overview.md       # (English)
    ├── 01_core_design.md           # (English)
    ├── 02_web_data.md               # (English)
    ├── 03_security_microservices.md # (English)
    ├── 04_test_tdd_bdd.md           # (English)
    ├── 05_devops_process.md         # (English)
    └── vi/                          # (Vietnamese version)
```

---

## 6. Skill Assessment Matrix

| Skill Group                       | Reference File                 | Weight |
|-----------------------------------|-------------------------------|--------|
| Core Architecture & Design       | `01_core_design.md`           | 25%    |
| Web API & Data Persistence       | `02_web_data.md`              | 25%    |
| Security & Distributed Systems   | `03_security_microservices.md`| 25%    |
| Quality Assurance & Testing      | `04_test_tdd_bdd.md`          | 15%    |
| DevOps & CI/CD                   | `05_devops_process.md`        | 10%    |

---

## 7. Main Data Flow

**Scenario: Create New Employee**
1. Client sends `POST /api/v1/employees` via API Gateway.
2. Gateway validates JWT → forwards to Employee Service.
3. Employee Service validates data → saves to PostgreSQL.
4. Publishes `EmployeeCreatedEvent` to Kafka topic `employee-events`.
5. Notification Service consumes event → sends welcome email.
6. Audit Log Service consumes event → records the audit entry.
7. Department Service consumes event → updates headcount asynchronously.

---

## 8. Detailed Reference Docs

| File                                | Content Summary                                   |
|-------------------------------------|---------------------------------------------------|
| [01_core_design.md](01_core_design.md)               | SOLID, Design Patterns, Java 21 features          |
| [02_web_data.md](02_web_data.md)                     | REST API, JPA, ERD, Swagger                       |
| [03_security_microservices.md](03_security_microservices.md) | JWT, Gateway, Kafka, Config Server          |
| [04_test_tdd_bdd.md](04_test_tdd_bdd.md)             | TDD, JUnit 5, Mockito, BDD/Cucumber              |
| [05_devops_process.md](05_devops_process.md)          | GitFlow, Jenkins, SonarQube, JIRA                 |
