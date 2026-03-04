# 🚀 Group 5: Development Process & CI/CD

## Table of Contents
1. [Git Workflow (Gitflow)](#1-gitflow)
2. [Jenkins Pipeline](#2-jenkins-pipeline)
3. [SonarQube Integration](#3-sonarqube)
4. [JIRA Task Management](#4-jira)

---

## 1. Gitflow

### 1.1. Branch Strategy

```
main ─────────────●──────────────────────●──────────── (Production)
                  │                      ▲
                  │                      │ merge
release/1.0 ─────┼──────●───────●───────┘
                  │      │       ▲
                  │      │ fix   │ merge
                  │      ▼       │
develop ──●───●───●──●───●───●───●────●──────── (Integration)
           │   │      │       │       │
           │   │      │       │       └─feature/EMS-30-salary-report
           │   │      │       └─feature/EMS-25-kafka-events
           │   │      └─feature/EMS-20-jwt-auth
           │   └─feature/EMS-15-employee-crud
           └─feature/EMS-10-project-setup
```

### 1.2. Branch Naming Convention

| Branch Type    | Pattern                        | Example                            |
|----------------|-------------------------------|------------------------------------|
| **main**       | `main`                        | Production-ready code              |
| **develop**    | `develop`                     | Integration branch                 |
| **feature**    | `feature/EMS-{id}-{desc}`    | `feature/EMS-15-employee-crud`     |
| **bugfix**     | `bugfix/EMS-{id}-{desc}`     | `bugfix/EMS-42-fix-null-salary`    |
| **release**    | `release/{version}`           | `release/1.0.0`                    |
| **hotfix**     | `hotfix/EMS-{id}-{desc}`     | `hotfix/EMS-99-fix-jwt-expiry`     |

### 1.3. Commit Message Convention

```
<type>(scope): <description>

[optional body]
[optional footer]
```

| Type       | Description                      |
|------------|----------------------------------|
| `feat`     | New feature                      |
| `fix`      | Bug fix                          |
| `refactor` | Code refactoring                 |
| `test`     | Add/update tests                 |
| `docs`     | Documentation                    |
| `chore`    | Build, CI, dependencies          |

**Example:**
```
feat(employee): add create employee API with validation

- Implement POST /api/v1/employees endpoint
- Add Bean Validation for CreateEmployeeRequest
- Publish EmployeeCreatedEvent to Kafka

Closes EMS-15
```

### 1.4. Pull Request Workflow

```
1. Create branch → git checkout -b feature/EMS-15-employee-crud develop
2. Develop      → Commit using convention
3. Push         → git push origin feature/EMS-15-employee-crud
4. Create PR    → Target: develop, Assign reviewers (2 people)
5. CI Runs      → Build + Test + SonarQube
6. Code Review  → At least 2 approvals
7. Merge        → Squash merge into develop
8. Cleanup      → Delete feature branch
```

---

## 2. Jenkins Pipeline

### 2.1. Jenkinsfile

```groovy
pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-21'
    }

    environment {
        SONAR_HOST = credentials('sonar-host-url')
        SONAR_TOKEN = credentials('sonar-token')
        DOCKER_REGISTRY = 'registry.company.com'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_MSG = sh(
                        script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                }
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean compile -DskipTests'
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'mvn test -Dtest="**/unit/**"'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Integration Tests') {
            steps {
                sh 'mvn verify -DskipUTs -Dtest="**/integration/**"'
            }
            post {
                always {
                    junit '**/target/failsafe-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        mvn sonar:sonar \
                            -Dsonar.projectKey=antigravity-ems \
                            -Dsonar.host.url=${SONAR_HOST} \
                            -Dsonar.token=${SONAR_TOKEN} \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Package') {
            when { branch 'develop' }
            steps {
                sh 'mvn package -DskipTests'
            }
        }

        stage('Docker Build & Push') {
            when { branch 'develop' }
            steps {
                script {
                    def image = docker.build("${DOCKER_REGISTRY}/ems-employee:${env.BUILD_NUMBER}")
                    docker.withRegistry("https://${DOCKER_REGISTRY}", 'docker-credentials') {
                        image.push()
                        image.push('latest')
                    }
                }
            }
        }

        stage('Deploy to DEV') {
            when { branch 'develop' }
            steps {
                sh 'kubectl apply -f k8s/dev/ --namespace=ems-dev'
            }
        }
    }

    post {
        success { slackSend(color: 'good', message: "✅ Build #${env.BUILD_NUMBER} PASSED") }
        failure { slackSend(color: 'danger', message: "❌ Build #${env.BUILD_NUMBER} FAILED") }
        always  { cleanWs() }
    }
}
```

### 2.2. Pipeline Stages Diagram

```
┌──────────┐  ┌───────┐  ┌───────────┐  ┌───────────────┐  ┌──────────┐
│ Checkout │─▶│ Build │─▶│Unit Tests │─▶│ Integration   │─▶│SonarQube │
└──────────┘  └───────┘  └───────────┘  │    Tests      │  │ Analysis │
                                         └───────────────┘  └────┬─────┘
                                                                 │
┌──────────────┐  ┌────────────────────┐  ┌──────────────┐      │
│ Deploy (DEV) │◀─│ Docker Build/Push  │◀─│   Package    │◀─────┘
└──────────────┘  └────────────────────┘  └──────────────┘
                                               ▲
                                     Quality Gate Pass ✅
```

---

## 3. SonarQube

### 3.1. Quality Gate Criteria

| Metric                      | Condition          | Target Value      |
|-----------------------------|--------------------|-------------------|
| **Coverage** (new code)     | ≥                  | 80%               |
| **Duplicated Lines**        | ≤                  | 3%                |
| **Maintainability Rating**  | =                  | A                 |
| **Reliability Rating**      | =                  | A                 |
| **Security Rating**         | =                  | A                 |
| **Bugs** (new code)         | =                  | 0                 |
| **Vulnerabilities**         | =                  | 0                 |
| **Code Smells** (new code)  | ≤                  | 5                 |

### 3.2. Maven Plugin Configuration

```xml
<!-- pom.xml -->
<properties>
    <sonar.projectKey>antigravity-ems</sonar.projectKey>
    <sonar.organization>ems-team</sonar.organization>
    <sonar.java.coveragePlugin>jacoco</sonar.java.coveragePlugin>
    <sonar.coverage.jacoco.xmlReportPaths>
        ${project.build.directory}/site/jacoco/jacoco.xml
    </sonar.coverage.jacoco.xmlReportPaths>
    <sonar.exclusions>
        **/config/**,**/dto/**,**/model/entity/**,**/mapper/**
    </sonar.exclusions>
</properties>

<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution><goals><goal>prepare-agent</goal></goals></execution>
        <execution>
            <id>report</id><phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

---

## 4. JIRA

### 4.1. Sprint Board Structure

```
┌────────────────────────────────────────────────────────────┐
│               Sprint 1: Foundation (2 weeks)               │
├──────────┬───────────┬────────────┬───────────┬───────────┤
│  TO DO   │IN PROGRESS│CODE REVIEW │  TESTING  │   DONE    │
├──────────┼───────────┼────────────┼───────────┼───────────┤
│EMS-12    │EMS-15     │EMS-10      │           │EMS-01     │
│EMS-13    │EMS-16     │            │           │EMS-05     │
│EMS-14    │           │            │           │           │
└──────────┴───────────┴────────────┴───────────┴───────────┘
```

### 4.2. Issue Types & Workflow

| Issue Type | Prefix    | Example                                 |
|------------|-----------|----------------------------------------|
| **Epic**   | —         | Employee Management Module             |
| **Story**  | EMS-xxx   | As HR, I want to create employees      |
| **Task**   | EMS-xxx   | Implement EmployeeController           |
| **Bug**    | EMS-xxx   | Fix: NullPointer in salary calculation |
| **Sub-task** | EMS-xxx | Write unit tests for EmployeeService   |

### 4.3. Sprint Plan for 5-6 Person Team

| Sprint           | Epic                  | Stories           | Assignee          |
|------------------|-----------------------|-------------------|-------------------|
| **Sprint 1**     | Project Setup         | Setup, CI/CD      | DevOps (1 person) |
| (2 weeks)        | Core Architecture     | Base classes       | Senior (1 person) |
|                  | Employee CRUD         | API + DB          | Backend (2 people)|
|                  | Security              | JWT + RBAC        | Senior (1 person) |
| **Sprint 2**     | Department Service    | CRUD + Tree       | Backend (1 person)|
| (2 weeks)        | Project Service       | CRUD + Assignment | Backend (1 person)|
|                  | Kafka Integration     | Events + Consumers| Senior (1 person) |
|                  | Testing               | TDD + BDD         | QA (1 person)     |
| **Sprint 3**     | Notification Service  | Email + Push      | Backend (1 person)|
| (2 weeks)        | Audit Log Service     | MongoDB logging   | Backend (1 person)|
|                  | Performance           | Virtual Threads   | Senior (1 person) |
|                  | Documentation         | API docs + README | All               |

### 4.4. Definition of Done (DoD)

- [ ] Code has been reviewed by at least 2 developers
- [ ] Unit test coverage ≥ 80% (on new code)
- [ ] Integration tests pass
- [ ] SonarQube Quality Gate: PASSED
- [ ] API documentation (Swagger) updated
- [ ] No Blocker or Critical bugs
- [ ] PR merged into develop
- [ ] JIRA ticket status updated to DONE
