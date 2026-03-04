# 🔒 Group 3: Security & Distributed Systems

## Table of Contents
1. [Spring Security & JWT](#1-spring-security--jwt)
2. [Microservices Architecture](#2-microservices-architecture)
3. [Kafka Event-Driven Communication](#3-kafka-event-driven-communication)
4. [Centralized Configuration](#4-centralized-configuration)

---

## 1. Spring Security & JWT

### 1.1. Authentication Flow

```
┌──────┐      ┌──────────┐       ┌──────────────┐      ┌─────────────┐
│Client│      │API Gateway│       │Auth Service   │      │Employee Svc │
└──┬───┘      └────┬─────┘       └──────┬───────┘      └──────┬──────┘
   │  POST /login  │                    │                     │
   │──────────────▶│  Forward           │                     │
   │               │───────────────────▶│                     │
   │               │                    │ Validate credentials│
   │               │                    │ Generate JWT        │
   │               │   JWT Token        │                     │
   │◀──────────────│◀───────────────────│                     │
   │               │                    │                     │
   │  GET /employees (Authorization: Bearer <JWT>)            │
   │──────────────▶│                    │                     │
   │               │ Validate JWT       │                     │
   │               │ Extract claims     │                     │
   │               │ Forward + SecurityContext                │
   │               │────────────────────────────────────────▶│
   │               │                    │                     │ Process
   │   200 OK      │◀────────────────────────────────────────│
   │◀──────────────│                    │                     │
```

### 1.2. JWT Token Structure

```json
// Header
{ "alg": "RS256", "typ": "JWT" }

// Payload
{
  "sub": "user-id-123",
  "email": "admin@ems.com",
  "roles": ["ROLE_ADMIN", "ROLE_HR"],
  "iss": "antigravity-ems",
  "iat": 1709568000,
  "exp": 1709571600
}
```

### 1.3. Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // For @PreAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)  // Stateless → no CSRF needed
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/employees/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/employees/**").hasRole("HR")
                .requestMatchers(HttpMethod.PUT, "/api/v1/employees/**").hasRole("HR")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/employees/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())))
            .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthConverter() {
        var grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.public-key}") RSAPublicKey publicKey) {
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }
}
```

### 1.4. Method-Level Security

```java
@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Override
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) { /*...*/ }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteEmployee(String id) { /*...*/ }

    @Override
    @PreAuthorize("#id == authentication.principal.claims['sub'] or hasRole('HR')")
    public EmployeeResponse findById(String id) { /*...*/ }
}
```

### 1.5. RBAC Matrix

| Endpoint              | USER | HR   | MANAGER | ADMIN |
|----------------------|------|------|---------|-------|
| GET /employees       | ✅   | ✅   | ✅      | ✅    |
| GET /employees/{id}  | Own  | ✅   | Team    | ✅    |
| POST /employees      | ❌   | ✅   | ❌      | ✅    |
| PUT /employees/{id}  | Own  | ✅   | ❌      | ✅    |
| DELETE /employees    | ❌   | ❌   | ❌      | ✅    |

---

## 2. Microservices Architecture

### 2.1. Service Communication

```
┌──────────────────────────────────────────────────────────────┐
│                   Spring Cloud Gateway (8080)                 │
│  Routes:                                                      │
│    /api/v1/employees/** → employee-service                   │
│    /api/v1/departments/** → department-service               │
│    /api/v1/projects/** → project-service                     │
└──────────────┬──────────────────────┬────────────────────────┘
               │ Sync (HTTP/REST)      │
    ┌──────────▼──────┐    ┌──────────▼──────┐
    │ Employee Service│    │Department Service│
    │     (8081)      │    │     (8082)       │
    └────────┬────────┘    └────────┬─────────┘
             │                      │
             │ Async (Kafka Events) │
             ▼                      ▼
    ┌─────────────────────────────────────────┐
    │          Apache Kafka Cluster           │
    │  Topics:                                │
    │    • employee-events                    │
    │    • department-events                  │
    │    • notification-topic                 │
    │    • audit-log-topic                    │
    └────────┬──────────────────┬─────────────┘
             ▼                  ▼
    ┌────────────────┐ ┌───────────────┐
    │Notification Svc│ │ Audit Log Svc │
    │   (MongoDB)    │ │   (MongoDB)   │
    └────────────────┘ └───────────────┘
```

### 2.2. Gateway Configuration

```yaml
# gateway application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: employee-service
          uri: lb://EMPLOYEE-SERVICE
          predicates:
            - Path=/api/v1/employees/**
          filters:
            - name: CircuitBreaker
              args:
                name: employeeCB
                fallbackUri: forward:/fallback/employees
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20

        - id: department-service
          uri: lb://DEPARTMENT-SERVICE
          predicates:
            - Path=/api/v1/departments/**

        - id: project-service
          uri: lb://PROJECT-SERVICE
          predicates:
            - Path=/api/v1/projects/**
```

### 2.3. Service Discovery (Eureka)

```yaml
# eureka-server application.yml
server.port: 8761
eureka:
  client:
    register-with-eureka: false
    fetch-registry: false

# client service application.yml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${random.value}
```

### 2.4. Inter-Service Communication (REST via WebClient)

```java
@Component
@RequiredArgsConstructor
public class DepartmentClient {
    private final WebClient.Builder webClientBuilder;

    public DepartmentResponse getDepartment(String departmentId) {
        return webClientBuilder.build()
                .get()
                .uri("http://DEPARTMENT-SERVICE/api/v1/departments/{id}", departmentId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                    Mono.error(new ResourceNotFoundException("Department", departmentId)))
                .bodyToMono(DepartmentResponse.class)
                .block();
    }
}
```

---

## 3. Kafka Event-Driven Communication

### 3.1. Kafka Configuration

```java
@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic employeeEventsTopic() {
        return TopicBuilder.name("employee-events")
                .partitions(3).replicas(1).build();
    }

    @Bean
    public ProducerFactory<String, DomainEvent> producerFactory(
            KafkaProperties properties) {
        Map<String, Object> config = properties.buildProducerProperties(null);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }
}
```

### 3.2. Event Publishing & Consuming

```java
// Producer
@Component
@RequiredArgsConstructor
public class EmployeeEventPublisher {
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

    public void publish(EmployeeEvent event) {
        kafkaTemplate.send("employee-events", event.employeeId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) log.error("Failed to publish event", ex);
                    else log.info("Published event: {}", event.eventId());
                });
    }
}

// Consumer — Notification Service
@Component
public class NotificationEventHandler {
    @KafkaListener(topics = "employee-events", groupId = "notification-svc")
    public void handle(EmployeeEvent event) {
        switch (event) {
            case EmployeeCreatedEvent e -> sendWelcomeEmail(e);
            case EmployeeDeletedEvent e -> sendFarewellEmail(e);
            default -> { }
        }
    }
}

// Consumer — Audit Log Service
@Component
public class AuditEventHandler {
    @KafkaListener(topics = "employee-events", groupId = "audit-svc")
    public void handle(EmployeeEvent event) {
        auditLogRepository.save(AuditLog.from(event));  // MongoDB
    }
}
```

### 3.3. Event Flow Diagram

```
Employee Service          Kafka              Notification Svc    Audit Svc
      │                     │                      │                │
      │ EmployeeCreatedEvent│                      │                │
      │────────────────────▶│                      │                │
      │                     │──────────────────────▶│ Send Email     │
      │                     │──────────────────────────────────────▶│ Save Log
      │                     │                      │                │
```

---

## 4. Centralized Configuration

### 4.1. Config Server Setup

```yaml
# config-server application.yml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/org/ems-config-repo
          default-label: main
          search-paths: '{application}'
server.port: 8888
```

### 4.2. Client Configuration

```yaml
# bootstrap.yml (client services)
spring:
  application.name: employee-service
  config:
    import: configserver:http://localhost:8888
  cloud:
    config:
      fail-fast: true
      retry:
        max-attempts: 5
        initial-interval: 1000
```

### 4.3. Config Repo Structure

```
ems-config-repo/
├── employee-service.yml          # Default profile
├── employee-service-dev.yml      # Dev profile
├── employee-service-prod.yml     # Prod profile
├── department-service.yml
├── project-service.yml
└── application.yml               # Shared config
```

### 4.4. Refresh Configuration at Runtime

```java
@RestController
@RefreshScope  // Hot-reload config when receiving POST /actuator/refresh
@RequiredArgsConstructor
public class FeatureFlagController {

    @Value("${feature.salary-report.enabled:false}")
    private boolean salaryReportEnabled;

    @GetMapping("/api/v1/features/salary-report")
    public boolean isSalaryReportEnabled() {
        return salaryReportEnabled;
    }
}
```

> **Production Note:** Use Spring Cloud Bus + Kafka to broadcast refresh events to all instances simultaneously instead of calling `/actuator/refresh` for each service individually.
