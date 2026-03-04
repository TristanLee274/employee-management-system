# 📐 Group 1: Core Architecture & Design Principles

## Table of Contents
1. [SOLID Principles in Antigravity-EMS](#1-solid-principles)
2. [Design Patterns](#2-design-patterns)
3. [Java 21 Features](#3-java-21-features)
4. [Package Structure & Layered Architecture](#4-package-structure)

---

## 1. SOLID Principles

### 1.1. Single Responsibility Principle (SRP)

> *A class should have one, and only one, reason to change.*

**Application in the project:**

```
com.ems.employee/
├── controller/EmployeeController.java    → Only handles HTTP request/response
├── service/EmployeeService.java          → Only contains business logic
├── repository/EmployeeRepository.java    → Only queries the database
├── mapper/EmployeeMapper.java            → Only converts Entity ↔ DTO
├── validator/EmployeeValidator.java      → Only validates business rules
└── event/EmployeeEventPublisher.java     → Only publishes Kafka events
```

**Code Example:**

```java
// ❌ Violates SRP — Controller doing too much
@RestController
public class EmployeeController {
    public ResponseEntity<?> createEmployee(EmployeeRequest req) {
        // Validate → Business logic → Save DB → Send email → Log audit
        // All in 1 method!
    }
}

// ✅ Follows SRP — Each class has one responsibility
@RestController
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    @PostMapping("/api/v1/employees")
    public ResponseEntity<EmployeeResponse> create(
            @Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(employeeService.createEmployee(request));
    }
}

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository repository;
    private final EmployeeMapper mapper;
    private final EmployeeEventPublisher eventPublisher;

    @Override
    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        Employee employee = mapper.toEntity(request);
        Employee saved = repository.save(employee);
        eventPublisher.publishCreatedEvent(saved);
        return mapper.toResponse(saved);
    }
}
```

---

### 1.2. Open/Closed Principle (OCP)

> *Software entities should be open for extension, but closed for modification.*

**Application: Strategy Pattern for Notification Types**

```java
// Interface open for extension
public interface NotificationSender {
    void send(NotificationPayload payload);
    NotificationType getType();
}

// Each notification type is a separate implementation
@Component
public class EmailNotificationSender implements NotificationSender {
    @Override
    public void send(NotificationPayload payload) {
        // Send email via SMTP
    }

    @Override
    public NotificationType getType() {
        return NotificationType.EMAIL;
    }
}

@Component
public class SmsNotificationSender implements NotificationSender {
    @Override
    public void send(NotificationPayload payload) {
        // Send SMS via Twilio
    }

    @Override
    public NotificationType getType() {
        return NotificationType.SMS;
    }
}

// ➕ Need to add Push Notification? Just create a new class, DO NOT modify old code
@Component
public class PushNotificationSender implements NotificationSender {
    @Override
    public void send(NotificationPayload payload) { /* ... */ }

    @Override
    public NotificationType getType() {
        return NotificationType.PUSH;
    }
}
```

---

### 1.3. Liskov Substitution Principle (LSP)

> *Subtypes must be substitutable for their base types without altering the correctness of the program.*

**Application: Employee Hierarchy**

```java
// Base class with a clear contract
public sealed interface Employee permits FullTimeEmployee, ContractEmployee, InternEmployee {
    String id();
    String fullName();
    BigDecimal calculateSalary();
    // All subtypes must be able to calculate salary — contract ensures LSP
}

public record FullTimeEmployee(String id, String fullName,
        BigDecimal baseSalary, BigDecimal bonus) implements Employee {
    @Override
    public BigDecimal calculateSalary() {
        return baseSalary.add(bonus);
    }
}

public record ContractEmployee(String id, String fullName,
        BigDecimal hourlyRate, int hoursWorked) implements Employee {
    @Override
    public BigDecimal calculateSalary() {
        return hourlyRate.multiply(BigDecimal.valueOf(hoursWorked));
    }
}

// Service can work with any Employee without knowing the concrete type
@Service
public class PayrollService {
    public BigDecimal calculateTotalPayroll(List<Employee> employees) {
        return employees.stream()
                .map(Employee::calculateSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

---

### 1.4. Interface Segregation Principle (ISP)

> *Clients should not be forced to depend upon interfaces that they do not use.*

```java
// ❌ Violates ISP — Interface is too large
public interface EmployeeOperations {
    Employee create(CreateEmployeeRequest req);
    Employee update(String id, UpdateEmployeeRequest req);
    void delete(String id);
    Employee findById(String id);
    Page<Employee> search(SearchCriteria criteria);
    byte[] exportToPdf(String id);
    void sendWelcomeEmail(String id);
    AuditLog getAuditLog(String id);
}

// ✅ Follows ISP — Segregated into smaller, specialized interfaces
public interface EmployeeCrudService {
    EmployeeResponse create(CreateEmployeeRequest request);
    EmployeeResponse update(String id, UpdateEmployeeRequest request);
    void delete(String id);
    EmployeeResponse findById(String id);
}

public interface EmployeeSearchService {
    Page<EmployeeResponse> search(EmployeeSearchCriteria criteria);
}

public interface EmployeeExportService {
    byte[] exportToPdf(String employeeId);
    byte[] exportToCsv(List<String> employeeIds);
}
```

---

### 1.5. Dependency Inversion Principle (DIP)

> *High-level modules should not depend on low-level modules. Both should depend on abstractions.*

```java
// Abstraction layer (domain/port)
public interface EmployeeRepository {
    Employee save(Employee employee);
    Optional<Employee> findById(String id);
    Page<Employee> findAll(Pageable pageable);
}

public interface EventPublisher {
    void publish(DomainEvent event);
}

// High-level module depends only on abstractions
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository repository;  // Interface, not JpaRepository
    private final EventPublisher eventPublisher;   // Interface, not KafkaTemplate

    @Override
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        // Business logic here — completely decoupled from infrastructure
    }
}

// Low-level modules implement the abstraction
@Repository
public class JpaEmployeeRepository implements EmployeeRepository {
    private final SpringDataEmployeeRepository springRepo;
    // Adapter pattern — wrap Spring Data
}

@Component
public class KafkaEventPublisher implements EventPublisher {
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    // Kafka-specific implementation
}
```

---

## 2. Design Patterns

### 2.1. Design Patterns Overview

| Pattern                | Where it is applied              | Reason                                           |
|------------------------|----------------------------------|--------------------------------------------------|
| **Factory Method**     | Entity/DTO creation              | Encapsulates complex object creation logic       |
| **Builder**            | Search criteria, complex DTOs    | Creates objects with multiple optional params   |
| **Strategy**           | Notification, Salary Calc        | Changes algorithm at runtime                     |
| **Observer (Kafka)**   | Event-driven communication       | Loose coupling between microservices             |
| **Template Method**    | Base CRUD service                | Reuses common flow, overrides specific details   |
| **Adapter**            | Repository, External API         | Wraps external lib, keeps domain clean           |
| **Decorator**          | Logging, Caching service         | Adds behavior without altering original code     |
| **Chain of Responsibility** | Validation pipeline        | Processes multiple validation rules sequentially |
| **Specification**      | Dynamic query building           | Composes complex query conditions safely         |

---

### 2.2. Factory Method Pattern

```java
// Factory to create EmployeeEvent by action type
public sealed interface EmployeeEvent extends DomainEvent
        permits EmployeeCreatedEvent, EmployeeUpdatedEvent, EmployeeDeletedEvent {

    static EmployeeEvent of(EventType type, Employee employee) {
        return switch (type) {
            case CREATED -> new EmployeeCreatedEvent(
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    employee.getId(),
                    employee.getFullName(),
                    employee.getDepartmentId()
            );
            case UPDATED -> new EmployeeUpdatedEvent(
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    employee.getId(),
                    employee.getChangedFields()
            );
            case DELETED -> new EmployeeDeletedEvent(
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    employee.getId()
            );
        };
    }
}

// Using Java 21 Record for Event
public record EmployeeCreatedEvent(
        String eventId,
        Instant timestamp,
        String employeeId,
        String fullName,
        String departmentId
) implements EmployeeEvent {}
```

---

### 2.3. Strategy Pattern

```java
// Strategy for salary calculation
public interface SalaryCalculationStrategy {
    BigDecimal calculate(Employee employee);
    EmployeeType getSupportedType();
}

@Component
public class FullTimeSalaryStrategy implements SalaryCalculationStrategy {
    @Override
    public BigDecimal calculate(Employee employee) {
        return employee.getBaseSalary()
                .add(employee.getAllowance())
                .subtract(employee.getDeductions());
    }

    @Override
    public EmployeeType getSupportedType() {
        return EmployeeType.FULL_TIME;
    }
}

@Component
public class ContractSalaryStrategy implements SalaryCalculationStrategy {
    @Override
    public BigDecimal calculate(Employee employee) {
        return employee.getHourlyRate()
                .multiply(BigDecimal.valueOf(employee.getHoursWorked()));
    }

    @Override
    public EmployeeType getSupportedType() {
        return EmployeeType.CONTRACT;
    }
}

// Context — uses Spring DI to inject all strategies
@Service
public class SalaryService {
    private final Map<EmployeeType, SalaryCalculationStrategy> strategies;

    public SalaryService(List<SalaryCalculationStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        SalaryCalculationStrategy::getSupportedType,
                        Function.identity()
                ));
    }

    public BigDecimal calculateSalary(Employee employee) {
        return Optional.ofNullable(strategies.get(employee.getType()))
                .orElseThrow(() -> new UnsupportedEmployeeTypeException(employee.getType()))
                .calculate(employee);
    }
}
```

---

### 2.4. Observer Pattern with Kafka

```java
// Publisher (Producer)
@Component
@RequiredArgsConstructor
public class EmployeeEventPublisher {
    private final KafkaTemplate<String, EmployeeEvent> kafkaTemplate;

    public void publishCreatedEvent(Employee employee) {
        var event = EmployeeEvent.of(EventType.CREATED, employee);
        kafkaTemplate.send("employee-events", employee.getId(), event);
    }
}

// Observer 1: Notification Service (Consumer)
@Component
@RequiredArgsConstructor
public class EmployeeCreatedNotificationHandler {
    private final NotificationSender emailSender;

    @KafkaListener(topics = "employee-events", groupId = "notification-service")
    public void handleEmployeeEvent(EmployeeEvent event) {
        switch (event) {
            case EmployeeCreatedEvent created ->
                emailSender.send(new WelcomeEmailPayload(
                        created.employeeId(),
                        created.fullName()
                ));
            case EmployeeDeletedEvent deleted ->
                emailSender.send(new FarewellEmailPayload(deleted.employeeId()));
            default -> { /* ignore other events */ }
        }
    }
}

// Observer 2: Audit Log Service (Consumer)
@Component
@RequiredArgsConstructor
public class EmployeeAuditLogHandler {
    private final AuditLogRepository auditLogRepository;

    @KafkaListener(topics = "employee-events", groupId = "audit-service")
    public void handleEmployeeEvent(EmployeeEvent event) {
        var auditLog = AuditLog.builder()
                .eventId(event.eventId())
                .eventType(event.getClass().getSimpleName())
                .timestamp(event.timestamp())
                .payload(event.toString())
                .build();
        auditLogRepository.save(auditLog);
    }
}
```

---

### 2.5. Template Method Pattern

```java
// Abstract base service for CRUD operations
public abstract class AbstractCrudService<E, ID, CreateReq, UpdateReq, Res> {

    protected abstract JpaRepository<E, ID> getRepository();
    protected abstract E mapToEntity(CreateReq request);
    protected abstract E updateEntity(E entity, UpdateReq request);
    protected abstract Res mapToResponse(E entity);
    protected abstract void publishEvent(EventType type, E entity);

    // Template Method — fixed flow, specific details can be overridden
    @Transactional
    public Res create(CreateReq request) {
        E entity = mapToEntity(request);
        validateBeforeSave(entity);           // Hook method
        E saved = getRepository().save(entity);
        afterSave(saved);                     // Hook method
        publishEvent(EventType.CREATED, saved);
        return mapToResponse(saved);
    }

    @Transactional
    public Res update(ID id, UpdateReq request) {
        E entity = getRepository().findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entity", id));
        E updated = updateEntity(entity, request);
        validateBeforeSave(updated);
        E saved = getRepository().save(updated);
        publishEvent(EventType.UPDATED, saved);
        return mapToResponse(saved);
    }

    // Hook methods — can be overridden
    protected void validateBeforeSave(E entity) { /* default: no-op */ }
    protected void afterSave(E entity) { /* default: no-op */ }
}

// Concrete service extending the template
@Service
public class EmployeeServiceImpl
        extends AbstractCrudService<Employee, String, CreateEmployeeRequest,
                                    UpdateEmployeeRequest, EmployeeResponse> {
    // Only need to implement abstract methods
    @Override
    protected Employee mapToEntity(CreateEmployeeRequest request) {
        return employeeMapper.toEntity(request);
    }

    @Override
    protected void validateBeforeSave(Employee employee) {
        if (repository.existsByEmail(employee.getEmail())) {
            throw new DuplicateResourceException("Employee", "email", employee.getEmail());
        }
    }
    // ... other abstract methods
}
```

---

### 2.6. Specification Pattern (Dynamic Query)

```java
// JPA Specification for dynamic filtering
public class EmployeeSpecification {

    public static Specification<Employee> withCriteria(EmployeeSearchCriteria criteria) {
        return Specification.where(hasName(criteria.name()))
                .and(hasDepartment(criteria.departmentId()))
                .and(hasStatus(criteria.status()))
                .and(joinedBetween(criteria.startDate(), criteria.endDate()));
    }

    private static Specification<Employee> hasName(String name) {
        return (root, query, cb) ->
                name == null ? null :
                cb.like(cb.lower(root.get("fullName")), "%" + name.toLowerCase() + "%");
    }

    private static Specification<Employee> hasDepartment(String deptId) {
        return (root, query, cb) ->
                deptId == null ? null :
                cb.equal(root.get("departmentId"), deptId);
    }

    private static Specification<Employee> hasStatus(EmployeeStatus status) {
        return (root, query, cb) ->
                status == null ? null :
                cb.equal(root.get("status"), status);
    }

    private static Specification<Employee> joinedBetween(LocalDate start, LocalDate end) {
        return (root, query, cb) -> {
            if (start == null && end == null) return null;
            if (start != null && end != null)
                return cb.between(root.get("joinDate"), start, end);
            if (start != null)
                return cb.greaterThanOrEqualTo(root.get("joinDate"), start);
            return cb.lessThanOrEqualTo(root.get("joinDate"), end);
        };
    }
}
```

---

## 3. Java 21 Features

### 3.1. Virtual Threads

```java
// Configure Spring Boot to use Virtual Threads (Tomcat)
// application.yml
spring:
  threads:
    virtual:
      enabled: true  # Spring Boot 3.2+ auto-configures virtual threads

// Or configure manually
@Configuration
public class VirtualThreadConfig {

    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }

    // Virtual Threads for Kafka Consumer
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
            kafkaListenerContainerFactory(ConsumerFactory<String, Object> consumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties()
                .setListenerTaskExecutor(
                        new SimpleAsyncTaskExecutor(Thread.ofVirtual().factory())
                );
        return factory;
    }
}
```

**Benefits of Virtual Threads:**
- Handles thousands of concurrent requests without thread pool tuning
- I/O-bound tasks (DB queries, HTTP calls, Kafka) automatically yield when blocked
- Reduces memory footprint compared to platform threads (~1KB vs ~1MB)

---

### 3.2. Records for DTOs

```java
// Request DTOs using Record — immutable, compact, auto-generates equals/hashCode/toString
public record CreateEmployeeRequest(
        @NotBlank(message = "Full name is required")
        String fullName,

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email,

        @NotNull(message = "Department ID is required")
        String departmentId,

        @NotNull(message = "Employee type is required")
        EmployeeType type,

        @Positive(message = "Salary must be positive")
        BigDecimal baseSalary
) {}

// Response DTOs
public record EmployeeResponse(
        String id,
        String fullName,
        String email,
        String departmentName,
        EmployeeType type,
        EmployeeStatus status,
        LocalDate joinDate,
        BigDecimal salary
) {}

// Search Criteria with default values
public record EmployeeSearchCriteria(
        String name,
        String departmentId,
        EmployeeStatus status,
        LocalDate startDate,
        LocalDate endDate,
        @Min(0) int page,
        @Min(1) @Max(100) int size
) {
    // Compact constructor for validation & defaults
    public EmployeeSearchCriteria {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 100) size = 100;
    }
}
```

---

### 3.3. Pattern Matching & Switch Expressions

```java
// Pattern Matching for instanceof (Java 16+)
public class EmployeeEventProcessor {

    public String processEvent(DomainEvent event) {
        // Pattern matching switch (Java 21 — finalized)
        return switch (event) {
            case EmployeeCreatedEvent e ->
                "New employee: %s joined department %s".formatted(e.fullName(), e.departmentId());

            case EmployeeUpdatedEvent e when e.changedFields().contains("salary") ->
                "Salary updated for employee: %s".formatted(e.employeeId());

            case EmployeeUpdatedEvent e ->
                "Employee %s updated fields: %s".formatted(e.employeeId(), e.changedFields());

            case EmployeeDeletedEvent e ->
                "Employee %s has been removed".formatted(e.employeeId());

            default -> "Unknown event type: %s".formatted(event.getClass().getSimpleName());
        };
    }
}

// Sealed classes combined with pattern matching
public sealed interface EmployeeStatus
        permits Active, OnLeave, Resigned, Terminated {

    record Active(LocalDate since) implements EmployeeStatus {}
    record OnLeave(LocalDate from, LocalDate to, String reason) implements EmployeeStatus {}
    record Resigned(LocalDate lastDay, String reason) implements EmployeeStatus {}
    record Terminated(LocalDate date, String reason) implements EmployeeStatus {}
}

public class EmployeeStatusHandler {
    public String getStatusMessage(EmployeeStatus status) {
        return switch (status) {
            case Active(var since) ->
                "Active since " + since;
            case OnLeave(var from, var to, var reason) ->
                "On leave: %s to %s (%s)".formatted(from, to, reason);
            case Resigned(var lastDay, _) ->
                "Resigned, last day: " + lastDay;
            case Terminated(var date, var reason) ->
                "Terminated on %s: %s".formatted(date, reason);
        };
    }
}
```

---

### 3.4. Structured Concurrency (Preview)

```java
// Structured Concurrency — Java 21 Preview Feature
// Used for parallel tasks with dependencies
@Service
public class EmployeeDashboardService {

    public EmployeeDashboardResponse getDashboard(String employeeId)
            throws InterruptedException, ExecutionException {

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // Fork 3 tasks in parallel
            Subtask<EmployeeProfile> profileTask =
                    scope.fork(() -> employeeClient.getProfile(employeeId));
            Subtask<List<Project>> projectsTask =
                    scope.fork(() -> projectClient.getProjectsByEmployee(employeeId));
            Subtask<AttendanceStats> attendanceTask =
                    scope.fork(() -> attendanceClient.getStats(employeeId));

            scope.join();           // Wait for all to complete
            scope.throwIfFailed();  // Propagate exception if any failed

            return new EmployeeDashboardResponse(
                    profileTask.get(),
                    projectsTask.get(),
                    attendanceTask.get()
            );
        }
    }
}
```

---

## 4. Package Structure

### Layered Architecture (per microservice)

```
com.ems.employee/
├── EmsEmployeeServiceApplication.java
│
├── controller/                  # Presentation Layer
│   ├── EmployeeController.java
│   └── advice/
│       └── GlobalExceptionHandler.java
│
├── service/                     # Business Logic Layer
│   ├── EmployeeService.java           (interface)
│   ├── impl/
│   │   └── EmployeeServiceImpl.java
│   └── SalaryCalculationStrategy.java (interface)
│
├── repository/                  # Data Access Layer
│   ├── EmployeeRepository.java        (Spring Data)
│   └── specification/
│       └── EmployeeSpecification.java
│
├── model/                       # Domain Layer
│   ├── entity/
│   │   ├── Employee.java
│   │   └── EmployeeStatus.java
│   └── enums/
│       └── EmployeeType.java
│
├── dto/                         # Data Transfer Objects
│   ├── request/
│   │   ├── CreateEmployeeRequest.java
│   │   └── UpdateEmployeeRequest.java
│   ├── response/
│   │   └── EmployeeResponse.java
│   └── criteria/
│       └── EmployeeSearchCriteria.java
│
├── mapper/                      # Object Mapping
│   └── EmployeeMapper.java
│
├── event/                       # Domain Events
│   ├── EmployeeEvent.java
│   ├── EmployeeCreatedEvent.java
│   └── EmployeeEventPublisher.java
│
├── config/                      # Configuration
│   ├── SecurityConfig.java
│   ├── KafkaConfig.java
│   └── SwaggerConfig.java
│
└── exception/                   # Custom Exceptions
    ├── ResourceNotFoundException.java
    ├── DuplicateResourceException.java
    └── BusinessRuleViolationException.java
```

> **Important Principle:** Dependencies always point **inwards** (Controller → Service → Repository). Repository never calls Service, and Service never calls Controller backward.
