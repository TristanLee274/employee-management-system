# 🧪 Nhóm 4: Quality Assurance & Testing

## Mục lục
1. [TDD — Test Driven Development](#1-tdd)
2. [Unit Testing (JUnit 5 + Mockito)](#2-unit-testing)
3. [BDD — Cucumber / Gherkin](#3-bdd--cucumber)

---

## 1. TDD

### 1.1. Quy trình Red → Green → Refactor

```
┌──────────────────────────────────────────────────────┐
│                  TDD CYCLE                           │
│                                                      │
│    ┌─────────┐    ┌─────────┐    ┌──────────┐       │
│    │  🔴 RED  │───▶│ 🟢 GREEN│───▶│ 🔵 REFACTOR│    │
│    │Write Test│    │Write Code│    │ Clean Up  │     │
│    │(FAIL)    │    │(PASS)    │    │(PASS)     │     │
│    └─────────┘    └─────────┘    └─────┬─────┘      │
│         ▲                              │             │
│         └──────────────────────────────┘             │
└──────────────────────────────────────────────────────┘
```

### 1.2. Ví dụ TDD: Tính năng "Create Employee"

**🔴 Bước 1: RED — Viết test trước**

```java
@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private EmployeeEventPublisher eventPublisher;
    @InjectMocks private EmployeeServiceImpl employeeService;

    @Test
    @DisplayName("Should create employee successfully when email is unique")
    void createEmployee_WhenEmailUnique_ShouldReturnResponse() {
        // Given
        var request = new CreateEmployeeRequest("Nguyen Van A",
                "nva@company.com", "dept-1", EmployeeType.FULL_TIME,
                new BigDecimal("25000000"));
        var entity = Employee.builder().id("emp-1")
                .fullName("Nguyen Van A").email("nva@company.com").build();
        var expected = new EmployeeResponse("emp-1", "Nguyen Van A",
                "nva@company.com", "Engineering", EmployeeType.FULL_TIME,
                EmployeeStatus.ACTIVE, LocalDate.now(), new BigDecimal("25000000"));

        when(employeeRepository.existsByEmail("nva@company.com")).thenReturn(false);
        when(employeeMapper.toEntity(request)).thenReturn(entity);
        when(employeeRepository.save(entity)).thenReturn(entity);
        when(employeeMapper.toResponse(entity)).thenReturn(expected);

        // When
        var result = employeeService.createEmployee(request);

        // Then
        assertThat(result).isEqualTo(expected);
        verify(eventPublisher).publishCreatedEvent(entity);
        verify(employeeRepository).save(entity);
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void createEmployee_WhenEmailDuplicate_ShouldThrowException() {
        var request = new CreateEmployeeRequest("Nguyen Van A",
                "existing@company.com", "dept-1", EmployeeType.FULL_TIME,
                new BigDecimal("25000000"));

        when(employeeRepository.existsByEmail("existing@company.com")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");

        verify(employeeRepository, never()).save(any());
        verify(eventPublisher, never()).publishCreatedEvent(any());
    }
}
```

**🟢 Bước 2: GREEN — Implement code tối thiểu**

```java
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final EmployeeEventPublisher eventPublisher;

    @Override
    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Employee", "email", request.email());
        }
        Employee entity = employeeMapper.toEntity(request);
        Employee saved = employeeRepository.save(entity);
        eventPublisher.publishCreatedEvent(saved);
        return employeeMapper.toResponse(saved);
    }
}
```

**🔵 Bước 3: REFACTOR — Tối ưu, dọn dẹp (tests vẫn phải pass)**

---

## 2. Unit Testing

### 2.1. Service Layer Tests

```java
@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock private EmployeeRepository repository;
    @Mock private EmployeeMapper mapper;
    @InjectMocks private EmployeeServiceImpl service;

    @Test
    void findById_WhenExists_ShouldReturn() {
        var entity = Employee.builder().id("1").fullName("Test").build();
        var response = new EmployeeResponse("1", "Test", null, null,
                null, null, null, null);
        when(repository.findById("1")).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        var result = service.findById("1");

        assertThat(result.id()).isEqualTo("1");
    }

    @Test
    void findById_WhenNotExists_ShouldThrow() {
        when(repository.findById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

### 2.2. Controller Layer Tests (MockMvc)

```java
@WebMvcTest(EmployeeController.class)
@Import(SecurityConfig.class)
class EmployeeControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private EmployeeService employeeService;

    @Test
    @WithMockUser(roles = "HR")
    void createEmployee_ValidRequest_ShouldReturn201() throws Exception {
        var request = """
            {
                "fullName": "Nguyen Van A",
                "email": "nva@company.com",
                "departmentId": "dept-1",
                "type": "FULL_TIME",
                "baseSalary": 25000000
            }
        """;
        var response = new EmployeeResponse("emp-1", "Nguyen Van A",
                "nva@company.com", "Engineering", EmployeeType.FULL_TIME,
                EmployeeStatus.ACTIVE, LocalDate.now(), new BigDecimal("25000000"));

        when(employeeService.createEmployee(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.email").value("nva@company.com"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createEmployee_WithUserRole_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createEmployee_InvalidRequest_ShouldReturn400() throws Exception {
        var invalidRequest = """
            { "fullName": "", "email": "invalid-email" }
        """;
        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }
}
```

### 2.3. Repository Layer Tests

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class EmployeeRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private EmployeeRepository repository;

    @Test
    void findByEmail_ShouldReturnEmployee() {
        var dept = Department.builder().name("Engineering").build();
        var emp = Employee.builder().fullName("Test").email("test@test.com")
                .type(EmployeeType.FULL_TIME).department(dept)
                .joinDate(LocalDate.now()).build();
        repository.save(emp);

        var result = repository.findByEmail("test@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getFullName()).isEqualTo("Test");
    }
}
```

---

## 3. BDD — Cucumber

### 3.1. Feature File (Gherkin)

```gherkin
# src/test/resources/features/employee_management.feature

Feature: Employee Management
  As an HR manager
  I want to manage employee records
  So that I can maintain accurate employee information

  Background:
    Given the system has department "Engineering" with id "dept-1"
    And I am authenticated as an HR manager

  Scenario: Successfully create a new employee
    When I create an employee with the following details:
      | fullName | email              | type      | salary   |
      | John Doe | john@company.com   | FULL_TIME | 25000000 |
    Then the employee should be created successfully
    And the response status code should be 201
    And the employee should belong to department "Engineering"

  Scenario: Reject duplicate email
    Given an employee with email "existing@company.com" already exists
    When I create an employee with the following details:
      | fullName  | email                | type      | salary   |
      | Jane Doe  | existing@company.com | FULL_TIME | 22000000 |
    Then the response status code should be 409
    And the error message should contain "email"

  Scenario Outline: Search employees by status
    Given the system has <total> employees with status "<status>"
    When I search for employees with status "<status>"
    Then I should receive <expected> results

    Examples:
      | status     | total | expected |
      | ACTIVE     | 10    | 10       |
      | ON_LEAVE   | 3     | 3        |
      | RESIGNED   | 5     | 5        |
```

### 3.2. Step Definitions

```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EmployeeStepDefinitions {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private DepartmentRepository departmentRepository;
    private ResponseEntity<?> lastResponse;

    @Given("the system has department {string} with id {string}")
    public void setupDepartment(String name, String id) {
        departmentRepository.save(Department.builder().id(id).name(name).build());
    }

    @Given("I am authenticated as an HR manager")
    public void authenticateAsHR() {
        // Set up JWT token with HR role
    }

    @When("I create an employee with the following details:")
    public void createEmployee(DataTable dataTable) {
        Map<String, String> data = dataTable.asMaps().get(0);
        var request = new CreateEmployeeRequest(
                data.get("fullName"), data.get("email"),
                "dept-1", EmployeeType.valueOf(data.get("type")),
                new BigDecimal(data.get("salary")));

        lastResponse = restTemplate.postForEntity(
                "/api/v1/employees", request, EmployeeResponse.class);
    }

    @Then("the employee should be created successfully")
    public void verifyCreated() {
        assertThat(lastResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Then("the response status code should be {int}")
    public void verifyStatusCode(int statusCode) {
        assertThat(lastResponse.getStatusCode().value()).isEqualTo(statusCode);
    }
}
```

### 3.3. Test Runner

```java
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.ems.employee.bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "pretty, html:target/cucumber-reports.html")
public class CucumberTestRunner {}
```

### 3.4. Testing Pyramid

```
            ┌───────────┐
            │  E2E / BDD │  ← Ít nhất, chậm nhất (Cucumber)
            │  (10-15%)  │
            ├───────────┤
            │Integration │  ← Trung bình (MockMvc, Testcontainers)
            │  (20-30%)  │
            ├───────────┤
            │ Unit Tests │  ← Nhiều nhất, nhanh nhất (JUnit + Mockito)
            │  (60-70%)  │
            └───────────┘
```

| Loại Test       | Tool                        | Scope            | Tốc độ  |
|-----------------|----------------------------|------------------|---------|
| Unit            | JUnit 5 + Mockito          | 1 class/method   | < 1ms   |
| Integration     | MockMvc + Testcontainers   | API endpoint     | < 5s    |
| E2E / BDD       | Cucumber + TestRestTemplate| Full user story  | < 30s   |
