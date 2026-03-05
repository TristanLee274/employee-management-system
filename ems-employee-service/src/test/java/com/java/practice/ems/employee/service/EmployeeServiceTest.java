package com.java.practice.ems.employee.service;

import com.java.practice.ems.employee.dto.*;
import com.java.practice.ems.employee.entity.Employee;
import com.java.practice.ems.employee.entity.EmployeeStatus;
import com.java.practice.ems.employee.exception.DuplicateEmailException;
import com.java.practice.ems.employee.exception.EmployeeNotFoundException;
import com.java.practice.ems.employee.repository.EmployeeRepository;
import com.java.practice.ems.employee.salary.SalaryCalculator;
import com.java.practice.ems.employee.salary.SalaryCalculatorFactory;
import com.java.practice.ems.employee.event.EventPublisher;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ EmployeeServiceTest — Comprehensive Unit Test Suite (JUnit 5 + Mockito) ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>TDD WORKFLOW: RED → GREEN → REFACTOR</h2>
 *
 * <p>
 * Test-Driven Development (TDD) is a discipline where tests are written BEFORE
 * the production code. The cycle is:
 * </p>
 *
 * <pre>
 *   ┌──────────────────────────────────────────────────────────────────────┐
 *   │                    TDD RED-GREEN-REFACTOR CYCLE                     │
 *   │                                                                     │
 *   │   ┌─────────┐         ┌─────────┐         ┌─────────────┐         │
 *   │   │  RED    │────────→│  GREEN  │────────→│  REFACTOR   │         │
 *   │   │         │         │         │         │             │         │
 *   │   │ Write a │         │ Write   │         │ Clean up    │         │
 *   │   │ failing │         │ minimal │         │ code while  │         │
 *   │   │ test    │         │ code    │         │ tests stay  │         │
 *   │   │ first   │         │ to pass │         │ green       │         │
 *   │   └─────────┘         └─────────┘         └──────┬──────┘         │
 *   │       ▲                                          │                 │
 *   │       └──────────────────────────────────────────┘                 │
 *   │                     Repeat for each feature                        │
 *   └──────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>
 * <strong>Impact on code quality:</strong>
 * </p>
 * <ul>
 * <li><strong>RED phase:</strong> Forces you to think about the API BEFORE
 * implementation — "What should this method accept? What should it return?
 * What errors should it throw?" This leads to cleaner interfaces.</li>
 * <li><strong>GREEN phase:</strong> You write ONLY what's needed to pass the
 * test — no speculative code, no over-engineering. YAGNI (You Ain't Gonna
 * Need It) is enforced automatically.</li>
 * <li><strong>REFACTOR phase:</strong> With a safety net of passing tests, you
 * can confidently restructure, rename, and optimize. Without tests, developers
 * fear refactoring — leading to code rot.</li>
 * </ul>
 *
 * <h2>MOCKITO — WHY WE MOCK DEPENDENCIES</h2>
 *
 * <p>
 * Unit tests must test ONE unit in ISOLATION. Without mocking, our
 * EmployeeServiceTest would need:
 * </p>
 * <ul>
 * <li>A running PostgreSQL database (infrastructure dependency)</li>
 * <li>A running Kafka broker (infrastructure dependency)</li>
 * <li>Actual salary calculators (separate unit responsibility)</li>
 * </ul>
 *
 * <p>
 * That would make these tests slow, flaky, and not truly "unit" tests — they'd
 * be integration tests. With Mockito mocks:
 * </p>
 * 
 * <pre>
 *   ┌───────────────────────────────────────────────────────────────────┐
 *   │  WITHOUT MOCKING (Integration Test-like)                         │
 *   │  EmployeeService → Real Repository → Real PostgreSQL             │
 *   │                  → Real KafkaProducer → Real Kafka Broker         │
 *   │  Problems: slow, requires infrastructure, tests multiple units   │
 *   │                                                                   │
 *   │  WITH MOCKING (True Unit Test)                                    │
 *   │  EmployeeService → Mock Repository (returns predefined data)     │
 *   │                  → Mock KafkaProducer (does nothing, is verified) │
 *   │  Benefits: fast (<1ms), no infrastructure, tests ONE unit         │
 *   └───────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>BDD (GIVEN-WHEN-THEN) — BRIDGING TECHNICAL CODE AND BUSINESS
 * REQUIREMENTS</h2>
 *
 * <p>
 * BDD structures tests so that non-technical stakeholders (Product Managers,
 * Team Leads) can read them and verify they match business requirements:
 * </p>
 * 
 * <pre>
 *   GIVEN (Arrange) — the preconditions:
 *     "Given an employee with email john@example.com exists in the system"
 *
 *   WHEN (Act) — the action:
 *     "When a new employee with the SAME email is created"
 *
 *   THEN (Assert) — the expected outcome:
 *     "Then a DuplicateEmailException should be thrown"
 * </pre>
 *
 * <p>
 * This is why we use Mockito BDD methods: {@code given()}, {@code then()},
 * and organize every test into GIVEN/WHEN/THEN sections with comments.
 * </p>
 *
 * <h2>CLEAN TEST CODE — TESTS AS FIRST-CLASS CITIZENS</h2>
 *
 * <p>
 * Tests are NOT throwaway code. They ARE production code that:
 * </p>
 * <ul>
 * <li><strong>Document behavior:</strong> Tests describe what the system does
 * better than any README — they're executable specifications.</li>
 * <li><strong>Prevent regressions:</strong> Every bug fixed has a test ensuring
 * it never returns. This is the "software immune system".</li>
 * <li><strong>Enable refactoring:</strong> Without tests, refactoring is
 * guesswork. With tests, it's confident restructuring.</li>
 * <li><strong>Reduce onboarding time:</strong> New team members read tests to
 * understand business rules faster than reading implementation code.</li>
 * </ul>
 *
 * <p>
 * <strong>Clean test principles applied here:</strong>
 * </p>
 * <ul>
 * <li>Method names follow {@code should_ExpectedBehavior_When_Condition}
 * naming</li>
 * <li>Each test verifies ONE behavior (Single Responsibility)</li>
 * <li>Tests are independent — no shared mutable state between tests</li>
 * <li>Arrange/Act/Assert sections are clearly commented</li>
 * <li>Helper methods reduce duplication without sacrificing readability</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
// @ExtendWith(MockitoExtension.class):
// JUnit 5 uses extensions instead of runners (JUnit 4's @RunWith).
// MockitoExtension:
// • Initializes @Mock, @InjectMocks, @Spy, @Captor annotations BEFORE each test
// • Validates that all stubbed methods are actually called (strict stubbing)
// • Reports unused stubs as test failures → keeps tests clean and intentional
@DisplayName("EmployeeService — Unit Tests")
// @DisplayName: Provides human-readable names in test reports.
// Instead of seeing "EmployeeServiceTest", the report shows
// "EmployeeService — Unit Tests" — readable by Team Leads and PMs.
@SuppressWarnings("null")
class EmployeeServiceTest {

        // ═══════════════════════════════════════════════════════════════════════════
        // MOCK DEPENDENCIES
        // ═══════════════════════════════════════════════════════════════════════════
        //
        // @Mock creates a Mockito mock object — a fake that records all interactions
        // and can be programmed to return specific values.
        //
        // WHY MOCK instead of real implementations?
        // → EmployeeRepository: We don't want a real DB. The mock returns whatever
        // we tell it to, instantly, without network I/O.
        // → SalaryCalculatorFactory: We don't want real calculation logic in a
        // SERVICE test. We test calculators separately.
        // → EmployeeEventProducer: We don't want a real Kafka broker running.
        // We just verify the event was sent with the correct data.

        @Mock
        private EmployeeRepository employeeRepository;

        @Mock
        private SalaryCalculatorFactory salaryCalculatorFactory;

        @Mock
        private EventPublisher eventPublisher;

        // @InjectMocks: Creates a REAL instance of EmployeeService and injects
        // the @Mock objects above via constructor injection.
        // This is the "unit under test" — everything else is mocked.
        @InjectMocks
        private EmployeeService employeeService;

        // @Captor: Captures arguments passed to mock methods for detailed assertion.
        // Instead of just verifying "save() was called", we capture the EXACT Employee
        // object passed to save() and assert on its fields.
        @Captor
        private ArgumentCaptor<Employee> employeeCaptor;

        // ═══════════════════════════════════════════════════════════════════════════
        // TEST FIXTURES — Reusable test data
        // ═══════════════════════════════════════════════════════════════════════════
        //
        // CLEAN TEST CODE: Extract common test data into constants and helper methods.
        // This reduces duplication and makes tests easier to read and maintain.
        // When a DTO changes (e.g., new field), update ONE helper method instead of 20
        // tests.

        private static final String EMPLOYEE_ID = "EMP-12345";
        private static final String FULL_NAME = "John Doe";
        private static final String EMAIL = "john.doe@company.com";
        private static final String PHONE = "+84-123-456-789";
        private static final String DEPARTMENT = "Engineering";
        private static final com.java.practice.ems.employee.entity.EmployeeType TYPE = com.java.practice.ems.employee.entity.EmployeeType.FULL_TIME;
        private static final String DEPARTMENT_ID = "DEPT-1";
        private static final BigDecimal SALARY = new BigDecimal("5000.00");
        private static final LocalDate HIRE_DATE = LocalDate.of(2024, 1, 15);

        /**
         * Creates a fully populated Employee entity for test scenarios.
         * <p>
         * CLEAN TEST CODE: Having a builder-style helper method ensures that every
         * test starts with a consistent, known-good entity. Changes to the Employee
         * class (e.g., adding a new mandatory field) require updating only this method.
         */
        private com.java.practice.ems.employee.entity.Department createTestDepartment() {
                com.java.practice.ems.employee.entity.Department dept = new com.java.practice.ems.employee.entity.Department();
                dept.setId(DEPARTMENT_ID);
                dept.setName(DEPARTMENT);
                return dept;
        }

        private Employee createTestEmployee() {
                Employee employee = Employee.builder()
                                .fullName(FULL_NAME)
                                .email(EMAIL)
                                .phone(PHONE)
                                .department(createTestDepartment())
                                .type(TYPE)
                                .baseSalary(SALARY)
                                .joinDate(HIRE_DATE)
                                .status(EmployeeStatus.ACTIVE)
                                .build();
                // id, createdAt, updatedAt are inherited from BaseEntity and
                // not included in Lombok's @Builder — set via reflection.
                org.springframework.test.util.ReflectionTestUtils.setField(employee, "id", EMPLOYEE_ID);
                org.springframework.test.util.ReflectionTestUtils.setField(employee, "createdAt",
                                java.time.LocalDateTime.now());
                org.springframework.test.util.ReflectionTestUtils.setField(employee, "updatedAt",
                                java.time.LocalDateTime.now());
                return employee;
        }

        /**
         * Creates a valid CreateEmployeeRequest for test scenarios.
         */
        private CreateEmployeeRequest createTestRequest() {
                return new CreateEmployeeRequest(
                                FULL_NAME, EMAIL, PHONE,
                                TYPE, DEPARTMENT_ID, SALARY, HIRE_DATE);
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // ╔══════════════════════════════════════════════════════════════════════════╗
        // ║ CREATE EMPLOYEE — TEST CASES ║
        // ╚══════════════════════════════════════════════════════════════════════════╝
        // ═══════════════════════════════════════════════════════════════════════════

        @Nested
        @DisplayName("create()")
        class CreateTests {

                @Test
                @DisplayName("should create employee and publish Kafka event when valid request")
                void should_CreateEmployeeAndPublishEvent_When_ValidRequest() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        // "Given no employee with this email exists"
                        CreateEmployeeRequest request = createTestRequest();
                        Employee savedEmployee = createTestEmployee();

                        given(employeeRepository.existsByEmail(EMAIL)).willReturn(false);
                        given(employeeRepository.save(any())).willReturn(savedEmployee);
                        // willDoNothing for the Kafka event producer — it's a void method;
                        // we just need to verify it was called with the correct event data.

                        // ── WHEN ──────────────────────────────────────────────────────────
                        // "When create is called with a valid employee request"
                        EmployeeResponse result = employeeService.create(request);

                        // ── THEN ──────────────────────────────────────────────────────────
                        // "Then the response should contain the saved employee's data"
                        assertThat(result).isNotNull();
                        assertThat(result.id()).isEqualTo(EMPLOYEE_ID);
                        assertThat(result.fullName()).isEqualTo(FULL_NAME);
                        assertThat(result.email()).isEqualTo(EMAIL);

                        // Verify the full interaction chain:
                        // 1. Email uniqueness was checked
                        then(employeeRepository).should().existsByEmail(EMAIL);

                        // 2. Employee entity was saved to database
                        then(employeeRepository).should().save(employeeCaptor.capture());
                        Employee capturedEmployee = employeeCaptor.getValue();
                        assertThat(capturedEmployee.getFullName()).isEqualTo(FULL_NAME);
                        assertThat(capturedEmployee.getEmail()).isEqualTo(EMAIL);
                        assertThat(capturedEmployee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);

                        // 3. Kafka event was published (Observer Pattern notification)
                        // We verify the event producer was called but don't test Kafka itself
                        // (that's an integration test concern). This is the unit boundary.
                        verify(eventPublisher).publishEvent(argThat(event -> event.eventType().equals("CREATED") &&
                                        event.employeeId().equals("E12345") &&
                                        event.employeeName().equals("Jane Doe")));
                }

                @Test
                @DisplayName("should throw DuplicateEmailException when email already exists")
                void should_ThrowDuplicateEmailException_When_EmailExists() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        // "Given an employee with email 'john.doe@company.com' already exists"
                        CreateEmployeeRequest request = createTestRequest();
                        given(employeeRepository.existsByEmail(EMAIL)).willReturn(true);

                        // ── WHEN / THEN ───────────────────────────────────────────────────
                        // "When creating an employee with the same email,
                        // Then a DuplicateEmailException should be thrown"
                        assertThatThrownBy(() -> employeeService.create(request))
                                        .isInstanceOf(DuplicateEmailException.class);

                        // Verify: save() was NEVER called — the duplicate check prevented it
                        then(employeeRepository).should(never()).save(any());
                        // Verify: Kafka event was NEVER published — no employee was created
                        then(eventPublisher).should(never()).publishEvent(any());
                }

                @Test
                @DisplayName("should default hire date to today when not provided")
                void should_DefaultHireDateToToday_When_NotProvided() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        // "Given a create request WITHOUT a hire date"
                        CreateEmployeeRequest requestWithoutDate = new CreateEmployeeRequest(
                                        FULL_NAME, EMAIL, PHONE,
                                        TYPE, DEPARTMENT_ID, SALARY, null);

                        Employee savedEmployee = createTestEmployee();
                        given(employeeRepository.existsByEmail(anyString())).willReturn(false);
                        given(employeeRepository.save(any())).willReturn(savedEmployee);

                        // ── WHEN ──────────────────────────────────────────────────────────
                        employeeService.create(requestWithoutDate);

                        // ── THEN ──────────────────────────────────────────────────────────
                        // "Then the saved employee's hire date should be today"
                        then(employeeRepository).should().save(employeeCaptor.capture());
                        Employee capturedEmployee = employeeCaptor.getValue();
                        assertThat(capturedEmployee.getJoinDate()).isEqualTo(LocalDate.now());
                }

                @Test
                @DisplayName("should always set new employee status to ACTIVE")
                void should_SetStatusToActive_When_CreatingNewEmployee() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        CreateEmployeeRequest request = createTestRequest();
                        Employee savedEmployee = createTestEmployee();

                        given(employeeRepository.existsByEmail(anyString())).willReturn(false);
                        given(employeeRepository.save(any())).willReturn(savedEmployee);

                        // ── WHEN ──────────────────────────────────────────────────────────
                        employeeService.create(request);

                        // ── THEN ──────────────────────────────────────────────────────────
                        // "Then the employee status should ALWAYS be ACTIVE for new hires"
                        then(employeeRepository).should().save(employeeCaptor.capture());
                        assertThat(employeeCaptor.getValue().getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
                }
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // ╔══════════════════════════════════════════════════════════════════════════╗
        // ║ UPDATE EMPLOYEE — TEST CASES ║
        // ╚══════════════════════════════════════════════════════════════════════════╝
        // ═══════════════════════════════════════════════════════════════════════════

        @Nested
        @DisplayName("update()")
        class UpdateTests {

                @Test
                @DisplayName("should update only provided fields (PATCH semantics)")
                void should_UpdateOnlyProvidedFields_When_PartialUpdateRequest() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        // "Given an existing employee and a request to update only the phone"
                        Employee existingEmployee = createTestEmployee();
                        given(employeeRepository.findById(EMPLOYEE_ID))
                                        .willReturn(Optional.of(existingEmployee));

                        // Only phone and department are being updated — all other fields are null
                        UpdateEmployeeRequest partialUpdate = new UpdateEmployeeRequest(null, null, "+84-999-888-777",
                                        null, "DEPT-2", null, null, null);

                        // ── WHEN ──────────────────────────────────────────────────────────
                        EmployeeResponse result = employeeService.update(EMPLOYEE_ID, partialUpdate);
                        assertThat(result).isNotNull();

                        // ── THEN ──────────────────────────────────────────────────────────
                        // "Then only phone and department should change, other fields preserved"
                        assertThat(existingEmployee.getPhone()).isEqualTo("+84-999-888-777");
                        assertThat(existingEmployee.getDepartment().getName()).isEqualTo("Product");
                        // These fields were NOT in the update request — they must remain unchanged
                        assertThat(existingEmployee.getFullName()).isEqualTo(FULL_NAME);
                        assertThat(existingEmployee.getFullName()).isEqualTo(FULL_NAME);
                        assertThat(existingEmployee.getEmail()).isEqualTo(EMAIL);
                        assertThat(existingEmployee.getBaseSalary()).isEqualByComparingTo(SALARY);
                }

                @Test
                @DisplayName("should throw EmployeeNotFoundException when updating non-existent employee")
                void should_ThrowException_When_UpdatingNonExistentEmployee() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        given(employeeRepository.findById("999")).willReturn(Optional.empty());
                        UpdateEmployeeRequest request = new UpdateEmployeeRequest("Jane", null, null, null, null, null,
                                        null, null);

                        // ── WHEN / THEN ───────────────────────────────────────────────────
                        assertThatThrownBy(() -> employeeService.update("999", request))
                                        .isInstanceOf(EmployeeNotFoundException.class);
                }

                @Test
                @DisplayName("should throw DuplicateEmailException when updating to existing email")
                void should_ThrowDuplicateEmail_When_UpdatingToExistingEmail() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        // "Given employee exists and another employee already uses the new email"
                        Employee existingEmployee = createTestEmployee();
                        given(employeeRepository.findById(EMPLOYEE_ID))
                                        .willReturn(Optional.of(existingEmployee));
                        given(employeeRepository.existsByEmail("taken@company.com"))
                                        .willReturn(true);

                        UpdateEmployeeRequest request = new UpdateEmployeeRequest(null, "taken@company.com", null, null,
                                        null, null,
                                        null, null);

                        // ── WHEN / THEN ───────────────────────────────────────────────────
                        assertThatThrownBy(() -> employeeService.update(EMPLOYEE_ID, request))
                                        .isInstanceOf(DuplicateEmailException.class);
                }

                @Test
                @DisplayName("should update status using valid status string")
                void should_UpdateStatus_When_ValidStatusProvided() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        Employee existingEmployee = createTestEmployee();
                        given(employeeRepository.findById(EMPLOYEE_ID))
                                        .willReturn(Optional.of(existingEmployee));

                        UpdateEmployeeRequest request = new UpdateEmployeeRequest(null, null, null, null, null, null,
                                        null, "ON_LEAVE");

                        // ── WHEN ──────────────────────────────────────────────────────────
                        employeeService.update(EMPLOYEE_ID, request);

                        // ── THEN ──────────────────────────────────────────────────────────
                        assertThat(existingEmployee.getStatus()).isEqualTo(EmployeeStatus.ON_LEAVE);
                }

                @Test
                @DisplayName("should throw IllegalArgumentException for invalid status string")
                void should_ThrowException_When_InvalidStatusProvided() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        Employee existingEmployee = createTestEmployee();
                        given(employeeRepository.findById(EMPLOYEE_ID))
                                        .willReturn(Optional.of(existingEmployee));

                        UpdateEmployeeRequest request = new UpdateEmployeeRequest(null, null, null, null, null, null,
                                        null, "INVALID_STATUS");

                        // ── WHEN / THEN ───────────────────────────────────────────────────
                        assertThatThrownBy(() -> employeeService.update(EMPLOYEE_ID, request))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Invalid employee status");
                }
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // ╔══════════════════════════════════════════════════════════════════════════╗
        // ║ DEACTIVATE EMPLOYEE — TEST CASES ║
        // ╚══════════════════════════════════════════════════════════════════════════╝
        // ═══════════════════════════════════════════════════════════════════════════

        @Nested
        @DisplayName("deactivate()")
        class DeactivateTests {

                @Test
                @DisplayName("should set status to INACTIVE when employee is ACTIVE")
                void should_SetStatusToInactive_When_EmployeeIsActive() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        Employee activeEmployee = createTestEmployee();
                        activeEmployee.setStatus(EmployeeStatus.ACTIVE);
                        given(employeeRepository.findById(EMPLOYEE_ID))
                                        .willReturn(Optional.of(activeEmployee));

                        // ── WHEN ──────────────────────────────────────────────────────────
                        employeeService.deactivate(EMPLOYEE_ID);

                        // ── THEN ────────────────────────────────────────────────────────── // THEN
                        assertThat(activeEmployee.getStatus()).isEqualTo(EmployeeStatus.INACTIVE); // State mutated
                        verify(eventPublisher).publishEvent(argThat(event -> event.eventType().equals("DEACTIVATED") &&
                                        event.employeeId().equals(EMPLOYEE_ID)));
                }

                @Test
                @DisplayName("should not change status when employee is already INACTIVE (idempotent)")
                void should_RemainInactive_When_AlreadyInactive() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        // BDD: "Given an employee who is already INACTIVE"
                        Employee inactiveEmployee = createTestEmployee();
                        inactiveEmployee.setStatus(EmployeeStatus.INACTIVE);
                        given(employeeRepository.findById(EMPLOYEE_ID))
                                        .willReturn(Optional.of(inactiveEmployee));

                        // ── WHEN ──────────────────────────────────────────────────────────
                        // "When deactivate is called"
                        employeeService.deactivate(EMPLOYEE_ID);

                        // ── THEN ──────────────────────────────────────────────────────────
                        // "Then status should remain INACTIVE (idempotent operation)"
                        assertThat(inactiveEmployee.getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
                        then(eventPublisher).should(never()).publishEvent(any()); // No event for idempotent operation
                }

                @Test
                @DisplayName("should throw IllegalStateException when deactivating TERMINATED employee")
                void should_ThrowException_When_DeactivatingTerminatedEmployee() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        Employee terminatedEmployee = createTestEmployee();
                        terminatedEmployee.setStatus(EmployeeStatus.TERMINATED);
                        given(employeeRepository.findById(EMPLOYEE_ID))
                                        .willReturn(Optional.of(terminatedEmployee));

                        // ── WHEN / THEN ───────────────────────────────────────────────────
                        assertThatThrownBy(() -> employeeService.deactivate(EMPLOYEE_ID))
                                        .isInstanceOf(IllegalStateException.class)
                                        .hasMessageContaining("Cannot deactivate a terminated employee");
                        then(eventPublisher).should(never()).publishEvent(any()); // No event if operation fails
                }

                @Test
                @DisplayName("should deactivate employee who is ON_LEAVE")
                void should_Deactivate_When_EmployeeIsOnLeave() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        Employee onLeaveEmployee = createTestEmployee();
                        onLeaveEmployee.setStatus(EmployeeStatus.ON_LEAVE);
                        given(employeeRepository.findById(EMPLOYEE_ID))
                                        .willReturn(Optional.of(onLeaveEmployee));

                        // ── WHEN ──────────────────────────────────────────────────────────
                        employeeService.deactivate(EMPLOYEE_ID);

                        // ── THEN ──────────────────────────────────────────────────────────
                        assertThat(onLeaveEmployee.getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
                }

                @Test
                @DisplayName("should throw EmployeeNotFoundException when deactivating non-existent employee")
                void should_ThrowException_When_DeactivatingNonExistentEmployee() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        given(employeeRepository.findById("999")).willReturn(Optional.empty());

                        // ── WHEN / THEN ───────────────────────────────────────────────────
                        assertThatThrownBy(() -> employeeService.deactivate("999"))
                                        .isInstanceOf(EmployeeNotFoundException.class);
                }
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // ╔══════════════════════════════════════════════════════════════════════════╗
        // ║ SALARY CALCULATION — TEST CASES ║
        // ╚══════════════════════════════════════════════════════════════════════════╝
        // ═══════════════════════════════════════════════════════════════════════════

        @Nested
        @DisplayName("calculateSalary()")
        class SalaryCalculationTests {

                @Test
                @DisplayName("should calculate salary using the correct strategy")
                void should_CalculateSalary_When_ValidEmployeeAndType() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        // "Given an employee with base salary 5000 and an HOURLY calculator"
                        Employee employee = createTestEmployee();
                        given(employeeRepository.findById(EMPLOYEE_ID))
                                        .willReturn(Optional.of(employee));

                        // Mock the Strategy Pattern: factory returns a mock calculator
                        SalaryCalculator mockCalculator = mock(SalaryCalculator.class);
                        given(salaryCalculatorFactory.getCalculator("HOURLY"))
                                        .willReturn(mockCalculator);
                        given(mockCalculator.calculate(any()))
                                        .willReturn(new BigDecimal("4500.00"));
                        given(mockCalculator.annualSalary(new BigDecimal("4500.00")))
                                        .willReturn(new BigDecimal("54000.00"));

                        // ── WHEN ──────────────────────────────────────────────────────────
                        SalaryCalculationResult result = employeeService.calculateSalary(EMPLOYEE_ID, "HOURLY", 160.0);

                        // ── THEN ──────────────────────────────────────────────────────────
                        assertThat(result.employeeId()).isEqualTo(EMPLOYEE_ID);
                        assertThat(result.employmentType()).isEqualTo("HOURLY");
                        assertThat(result.baseSalary()).isEqualByComparingTo(SALARY);
                        assertThat(result.netMonthlySalary()).isEqualByComparingTo("4500.00");
                        assertThat(result.netAnnualSalary()).isEqualByComparingTo("54000.00");
                        assertThat(result.hoursWorked()).isEqualTo(160.0);

                        // Verify the Strategy Pattern interaction chain:
                        then(salaryCalculatorFactory).should().getCalculator("HOURLY");
                        then(mockCalculator).should().calculate(any());
                        then(mockCalculator).should().annualSalary(new BigDecimal("4500.00"));
                }

                @Test
                @DisplayName("should throw EmployeeNotFoundException for non-existent employee")
                void should_ThrowException_When_EmployeeNotFoundForSalaryCalc() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        given(employeeRepository.findById("999")).willReturn(Optional.empty());

                        // ── WHEN / THEN ───────────────────────────────────────────────────
                        assertThatThrownBy(() -> employeeService.calculateSalary("999", "MONTHLY", 0))
                                        .isInstanceOf(EmployeeNotFoundException.class);

                        // Verify: Factory was NEVER called — employee lookup failed first
                        then(salaryCalculatorFactory).shouldHaveNoInteractions();
                }

                @Test
                @DisplayName("should throw IllegalStateException when employee has no base salary")
                void should_ThrowException_When_NoBaseSalaryConfigured() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        // "Given an employee with NULL salary"
                        Employee employeeWithNoSalary = createTestEmployee();
                        employeeWithNoSalary.setBaseSalary(null);
                        given(employeeRepository.findById(EMPLOYEE_ID))
                                        .willReturn(Optional.of(employeeWithNoSalary));

                        // ── WHEN / THEN ───────────────────────────────────────────────────
                        assertThatThrownBy(() -> employeeService.calculateSalary(EMPLOYEE_ID, "MONTHLY", 0))
                                        .isInstanceOf(IllegalStateException.class)
                                        .hasMessageContaining("no base salary configured");
                }

                @Test
                @DisplayName("should throw IllegalStateException when salary is zero")
                void should_ThrowException_When_SalaryIsZero() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        Employee employeeWithZeroSalary = createTestEmployee();
                        employeeWithZeroSalary.setBaseSalary(BigDecimal.ZERO);
                        given(employeeRepository.findById(EMPLOYEE_ID))
                                        .willReturn(Optional.of(employeeWithZeroSalary));

                        // ── WHEN / THEN ───────────────────────────────────────────────────
                        assertThatThrownBy(() -> employeeService.calculateSalary(EMPLOYEE_ID, "MONTHLY", 0))
                                        .isInstanceOf(IllegalStateException.class);
                }
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // ╔══════════════════════════════════════════════════════════════════════════╗
        // ║ FIND ALL (PAGINATION) — TEST CASES ║
        // ╚══════════════════════════════════════════════════════════════════════════╝
        // ═══════════════════════════════════════════════════════════════════════════

        @Nested
        @DisplayName("findAll()")
        class FindAllTests {

                @Test
                @DisplayName("should return paginated employee list with ASC sorting")
                void should_ReturnPaginatedList_When_ValidPaginationParams() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        Employee employee = createTestEmployee();
                        Page<Employee> page = new PageImpl<>(
                                        java.util.List.of(employee),
                                        PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "lastName")),
                                        1);
                        given(employeeRepository.findAll(any(Pageable.class))).willReturn(page);

                        // ── WHEN ──────────────────────────────────────────────────────────
                        Page<EmployeeListResponse> result = employeeService.findAll(0, 10, "lastName", "ASC");

                        // ── THEN ──────────────────────────────────────────────────────────
                        assertThat(result.getTotalElements()).isEqualTo(1);
                        assertThat(result.getContent()).hasSize(1);
                        assertThat(result.getContent().getFirst().fullName()).isEqualTo(FULL_NAME);

                        then(employeeRepository).should().findAll(any(Pageable.class));
                }

                @Test
                @DisplayName("should handle DESC sorting direction")
                void should_SortDescending_When_DirectionIsDESC() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        Page<Employee> emptyPage = Page.empty();
                        given(employeeRepository.findAll(any(Pageable.class))).willReturn(emptyPage);

                        // ── WHEN ──────────────────────────────────────────────────────────
                        employeeService.findAll(0, 10, "salary", "DESC");

                        // ── THEN ──────────────────────────────────────────────────────────
                        // Verify the Pageable passed to the repository has DESC sort
                        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
                        then(employeeRepository).should().findAll(pageableCaptor.capture());
                        Pageable capturedPageable = pageableCaptor.getValue();
                        assertThat(capturedPageable.getSort().getOrderFor("salary"))
                                        .isNotNull()
                                        .satisfies(order -> assertThat(order.getDirection())
                                                        .isEqualTo(Sort.Direction.DESC));
                }

                @Test
                @DisplayName("should default to ASC when direction is null or invalid")
                void should_DefaultToASC_When_DirectionIsNullOrInvalid() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        Page<Employee> emptyPage = Page.empty();
                        given(employeeRepository.findAll(any(Pageable.class))).willReturn(emptyPage);

                        // ── WHEN ──────────────────────────────────────────────────────────
                        employeeService.findAll(0, 10, "lastName", null);

                        // ── THEN ──────────────────────────────────────────────────────────
                        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
                        then(employeeRepository).should().findAll(pageableCaptor.capture());
                        Pageable capturedPageable = pageableCaptor.getValue();
                        assertThat(capturedPageable.getSort().getOrderFor("lastName"))
                                        .isNotNull()
                                        .satisfies(order -> assertThat(order.getDirection())
                                                        .isEqualTo(Sort.Direction.ASC));
                }
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // ╔══════════════════════════════════════════════════════════════════════════╗
        // ║ SEARCH — TEST CASES ║
        // ╚══════════════════════════════════════════════════════════════════════════╝
        // ═══════════════════════════════════════════════════════════════════════════

        @Nested
        @DisplayName("search()")
        class SearchTests {

                @Test
                @DisplayName("should return matching employees when keyword matches")
                void should_ReturnMatchingEmployees_When_KeywordMatches() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        Employee employee = createTestEmployee();
                        Page<Employee> page = new PageImpl<>(java.util.List.of(employee));
                        given(employeeRepository.searchByKeyword(eq("John"), any()))
                                        .willReturn(page);

                        // ── WHEN ──────────────────────────────────────────────────────────
                        Page<EmployeeListResponse> result = employeeService.search("John", 0, 10);

                        // ── THEN ──────────────────────────────────────────────────────────
                        assertThat(result.getTotalElements()).isEqualTo(1);
                        assertThat(result.getContent().getFirst().id()).isEqualTo(EMPLOYEE_ID);
                        assertThat(result.getContent().getFirst().fullName()).isEqualTo(FULL_NAME);
                }

                @Test
                @DisplayName("should return empty page when no employees match keyword")
                void should_ReturnEmptyPage_When_NoMatchFound() {
                        // ── GIVEN ─────────────────────────────────────────────────────────
                        given(employeeRepository.searchByKeyword(eq("NonExistent"), any()))
                                        .willReturn(Page.empty());

                        // ── WHEN ──────────────────────────────────────────────────────────
                        Page<EmployeeListResponse> result = employeeService.search("NonExistent", 0, 10);

                        // ── THEN ──────────────────────────────────────────────────────────
                        assertThat(result.getTotalElements()).isZero();
                        assertThat(result.getContent()).isEmpty();
                }
        }
}
