# 🌐 Nhóm 2: Web API & Data Persistence

## Mục lục
1. [RESTful API Design](#1-restful-api-design)
2. [Spring Data JPA & PostgreSQL](#2-spring-data-jpa--postgresql)
3. [Entity Relationship Diagram (ERD)](#3-erd)
4. [Swagger / OpenAPI 3](#4-swagger--openapi-3)

---

## 1. RESTful API Design

### 1.1. API Endpoints

| Method   | URI                                  | Mô tả                        | Status |
|----------|--------------------------------------|-------------------------------|--------|
| `GET`    | `/api/v1/employees`                 | Danh sách (pagination)         | 200    |
| `GET`    | `/api/v1/employees/{id}`            | Chi tiết 1 nhân viên           | 200    |
| `POST`   | `/api/v1/employees`                 | Tạo mới                       | 201    |
| `PUT`    | `/api/v1/employees/{id}`            | Cập nhật toàn bộ              | 200    |
| `PATCH`  | `/api/v1/employees/{id}`            | Cập nhật một phần             | 200    |
| `DELETE` | `/api/v1/employees/{id}`            | Soft delete                   | 204    |
| `GET`    | `/api/v1/employees/search`          | Tìm kiếm nâng cao             | 200    |
| `GET`    | `/api/v1/departments/{id}/employees`| Nhân viên theo phòng ban       | 200    |
| `POST`   | `/api/v1/projects/{id}/assignments` | Phân công vào dự án            | 201    |

### 1.2. Controller Implementation

```java
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employee", description = "Employee Management APIs")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<PageResponse<EmployeeResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        var pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortDir), sortBy));
        return ResponseEntity.ok(employeeService.findAll(pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<EmployeeResponse> create(
            @Valid @RequestBody CreateEmployeeRequest request) {
        var response = employeeService.createEmployee(request);
        var location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@PathVariable String id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 1.3. Standardized Response

```java
public record ApiResponse<T>(boolean success, String message, T data, Instant timestamp) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, Instant.now());
    }
}

public record PageResponse<T>(
    List<T> content, int pageNumber, int pageSize,
    long totalElements, int totalPages, boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}

public record ErrorResponse(int status, String error, String message,
                             Map<String, String> details, Instant timestamp) {
    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(),
                message, Map.of(), Instant.now());
    }
}
```

### 1.4. Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        f -> Objects.requireNonNullElse(f.getDefaultMessage(), "Invalid")));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "Bad Request", "Validation failed",
                        errors, Instant.now()));
    }
}
```

---

## 2. Spring Data JPA & PostgreSQL

### 2.1. Base Entity (Auditing)

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @CreatedDate @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy @Column(updatable = false)
    private String createdBy;

    @Version
    private Long version;  // Optimistic Locking

    private boolean deleted = false;  // Soft Delete
}
```

### 2.2. Entity Classes

```java
@Entity @Table(name = "employees")
@Getter @Setter @Builder
public class Employee extends BaseEntity {
    @Column(nullable = false, length = 100) private String fullName;
    @Column(nullable = false, unique = true) private String email;
    private String phone;
    @Enumerated(EnumType.STRING) private EmployeeType type;
    @Enumerated(EnumType.STRING) private EmployeeStatus status;
    private BigDecimal baseSalary;
    private LocalDate joinDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToMany(mappedBy = "employees")
    private Set<Project> projects = new HashSet<>();
}

@Entity @Table(name = "departments")
@Getter @Setter @Builder
public class Department extends BaseEntity {
    @Column(nullable = false, unique = true) private String name;
    private String code;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id")
    private Department parent;  // Self-ref tree
    @OneToMany(mappedBy = "department") private List<Employee> employees;
}

@Entity @Table(name = "projects")
@Getter @Setter @Builder
public class Project extends BaseEntity {
    @Column(nullable = false) private String name;
    private String description;
    @Enumerated(EnumType.STRING) private ProjectStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;

    @ManyToMany @JoinTable(name = "project_assignments",
        joinColumns = @JoinColumn(name = "project_id"),
        inverseJoinColumns = @JoinColumn(name = "employee_id"))
    private Set<Employee> employees = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
}
```

### 2.3. Repository Layer

```java
public interface EmployeeRepository extends JpaRepository<Employee, String>,
        JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT e FROM Employee e WHERE e.deleted = false AND e.department.id = :deptId")
    List<Employee> findActiveByDepartmentId(@Param("deptId") String deptId);

    @Query(value = """
        SELECT d.name AS department_name, COUNT(e.id) AS employee_count,
               AVG(e.base_salary) AS avg_salary
        FROM employees e JOIN departments d ON e.department_id = d.id
        WHERE e.deleted = false GROUP BY d.name
    """, nativeQuery = true)
    List<DepartmentStatsProjection> getDepartmentStatistics();
}
```

### 2.4. JPA Configuration (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ems_db
    username: ${DB_USERNAME:ems_user}
    password: ${DB_PASSWORD:ems_pass}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  jpa:
    hibernate.ddl-auto: validate
    properties.hibernate:
      dialect: org.hibernate.dialect.PostgreSQLDialect
      default_batch_fetch_size: 16
      jdbc.batch_size: 25
    open-in-view: false  # ⚠️ Best practice: tắt OSIV
  flyway:
    enabled: true
    locations: classpath:db/migration
```

### 2.5. Flyway Migration Scripts

```sql
-- V1__create_departments.sql
CREATE TABLE departments (
    id VARCHAR(36) PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(20), description VARCHAR(500),
    parent_id VARCHAR(36) REFERENCES departments(id),
    created_at TIMESTAMP DEFAULT NOW(), updated_at TIMESTAMP DEFAULT NOW(),
    version BIGINT DEFAULT 0, deleted BOOLEAN DEFAULT FALSE
);

-- V2__create_employees.sql
CREATE TABLE employees (
    id VARCHAR(36) PRIMARY KEY, full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE, phone VARCHAR(20),
    type VARCHAR(20) NOT NULL, status VARCHAR(20) DEFAULT 'ACTIVE',
    base_salary DECIMAL(15,2), join_date DATE NOT NULL,
    department_id VARCHAR(36) NOT NULL REFERENCES departments(id),
    created_at TIMESTAMP DEFAULT NOW(), updated_at TIMESTAMP DEFAULT NOW(),
    version BIGINT DEFAULT 0, deleted BOOLEAN DEFAULT FALSE
);
CREATE INDEX idx_emp_dept ON employees(department_id);

-- V3__create_projects.sql
CREATE TABLE projects (
    id VARCHAR(36) PRIMARY KEY, name VARCHAR(200) NOT NULL,
    description VARCHAR(1000), status VARCHAR(20) DEFAULT 'PLANNING',
    start_date DATE NOT NULL, end_date DATE, budget DECIMAL(18,2),
    department_id VARCHAR(36) REFERENCES departments(id),
    created_at TIMESTAMP DEFAULT NOW(), updated_at TIMESTAMP DEFAULT NOW(),
    version BIGINT DEFAULT 0, deleted BOOLEAN DEFAULT FALSE
);

-- V4__create_project_assignments.sql
CREATE TABLE project_assignments (
    project_id VARCHAR(36) REFERENCES projects(id),
    employee_id VARCHAR(36) REFERENCES employees(id),
    assigned_at TIMESTAMP DEFAULT NOW(), role VARCHAR(50),
    PRIMARY KEY (project_id, employee_id)
);
```

---

## 3. ERD

```
┌──────────────┐        ┌──────────────┐        ┌──────────────┐
│  departments │        │  employees   │        │   projects   │
├──────────────┤   1  N ├──────────────┤        ├──────────────┤
│ PK id        │───────▶│ PK id        │        │ PK id        │
│    name      │        │    full_name │        │    name      │
│    code      │        │    email     │   N  M │    status    │
│ FK parent_id │◀──┐    │    type      │◀──────▶│    budget    │
│    ...       │   │    │    status    │  via   │ FK dept_id   │
└──────────────┘   │    │    salary    │assign  │    ...       │
                   │    │ FK dept_id   │        └──────────────┘
                   │    │    ...       │
                   │    └──────────────┘   ┌──────────────────┐
                   │                       │project_assignments│
                   └───self-ref            │ FK project_id    │
                                           │ FK employee_id   │
                                           │    role          │
                                           └──────────────────┘
```

| Quan hệ | Loại | Mô tả |
|---------|------|-------|
| Department → Employee | 1:N | 1 phòng ban nhiều nhân viên |
| Department → Department | Self-ref | Cây phòng ban |
| Department → Project | 1:N | 1 phòng ban quản lý nhiều dự án |
| Employee ↔ Project | M:N | Qua bảng `project_assignments` |

---

## 4. Swagger / OpenAPI 3

### 4.1. Configuration

```java
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info().title("Antigravity-EMS API")
                .version("1.0.0").description("Employee Management System"))
            .addSecurityItem(new SecurityRequirement().addList("Bearer"))
            .components(new Components().addSecuritySchemes("Bearer",
                new SecurityScheme().type(SecurityScheme.Type.HTTP)
                    .bearerFormat("JWT").scheme("bearer")));
    }
}
```

### 4.2. application.yml

```yaml
springdoc:
  api-docs.path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    try-it-out-enabled: true
    operations-sorter: method
```

**Truy cập:** `http://localhost:8081/swagger-ui.html`
