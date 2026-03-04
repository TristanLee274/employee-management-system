# 🏗️ Antigravity-EMS — Project Overview

## 1. Giới thiệu

**Antigravity-EMS** là một dự án tham chiếu (Reference Project) được thiết kế để đào tạo và đánh giá ứng viên **Senior Java Developer**. Hệ thống quản lý nhân sự (Employee Management System) được xây dựng theo **kiến trúc Microservices**, áp dụng đầy đủ các nguyên lý kỹ thuật phần mềm hiện đại.

---

## 2. Sơ đồ Kiến trúc Tổng quan

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

| Thành phần            | Công nghệ                              | Phiên bản   |
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

## 4. Danh sách Microservices

| #  | Service              | Port  | Database   | Mô tả                              |
|----|----------------------|-------|------------|-------------------------------------|
| 1  | API Gateway          | 8080  | —          | Routing, Rate Limiting, JWT Validation |
| 2  | Employee Service     | 8081  | PostgreSQL | Quản lý thông tin nhân viên          |
| 3  | Department Service   | 8082  | PostgreSQL | Quản lý phòng ban                    |
| 4  | Project Service      | 8083  | PostgreSQL | Quản lý dự án & phân công            |
| 5  | Notification Service | 8084  | MongoDB    | Gửi thông báo (Email, SMS, Push)     |
| 6  | Audit Log Service    | 8085  | MongoDB    | Ghi log hoạt động & audit trail      |
| 7  | Config Server        | 8888  | Git repo   | Quản lý cấu hình tập trung          |
| 8  | Discovery Server     | 8761  | —          | Service registry (Eureka)            |

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
    ├── 00_project_overview.md
    ├── 01_core_design.md
    ├── 02_web_data.md
    ├── 03_security_microservices.md
    ├── 04_test_tdd_bdd.md
    └── 05_devops_process.md
```

---

## 6. Ma trận Đánh giá Kỹ năng

| Nhóm Kỹ năng                      | File Tham chiếu                | Trọng số |
|-----------------------------------|-------------------------------|----------|
| Core Architecture & Design       | `01_core_design.md`           | 25%      |
| Web API & Data Persistence       | `02_web_data.md`              | 25%      |
| Security & Distributed Systems   | `03_security_microservices.md`| 25%      |
| Quality Assurance & Testing      | `04_test_tdd_bdd.md`          | 15%      |
| DevOps & CI/CD                   | `05_devops_process.md`        | 10%      |

---

## 7. Luồng Dữ liệu Chính

```
[Client] ──HTTP──▶ [API Gateway] ──Route──▶ [Employee Service]
                                                    │
                                            ┌───────┴───────┐
                                            ▼               ▼
                                     [PostgreSQL]     [Kafka Producer]
                                                            │
                                            ┌───────────────┼───────────────┐
                                            ▼               ▼               ▼
                                    [Notification]   [Audit Log]    [Department Svc]
                                    [Service]        [Service]      (Sync data)
                                            │               │
                                            ▼               ▼
                                        [MongoDB]       [MongoDB]
```

**Kịch bản: Tạo nhân viên mới**
1. Client gửi `POST /api/v1/employees` qua API Gateway
2. Gateway xác thực JWT → forward đến Employee Service
3. Employee Service validate dữ liệu → lưu vào PostgreSQL
4. Publish event `EmployeeCreatedEvent` lên Kafka topic `employee-events`
5. Notification Service consume event → gửi email chào mừng
6. Audit Log Service consume event → ghi log thao tác tạo
7. Department Service consume event → cập nhật headcount

---

## 8. Tài liệu Chi tiết

| File                                | Nội dung                                          |
|-------------------------------------|--------------------------------------------------|
| [01_core_design.md](01_core_design.md)               | SOLID, Design Patterns, Java 21 features          |
| [02_web_data.md](02_web_data.md)                     | REST API, JPA, ERD, Swagger                       |
| [03_security_microservices.md](03_security_microservices.md) | JWT, Gateway, Kafka, Config Server          |
| [04_test_tdd_bdd.md](04_test_tdd_bdd.md)             | TDD, JUnit 5, Mockito, BDD/Cucumber              |
| [05_devops_process.md](05_devops_process.md)          | GitFlow, Jenkins, SonarQube, JIRA                 |
