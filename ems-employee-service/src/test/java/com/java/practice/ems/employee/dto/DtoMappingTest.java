package com.java.practice.ems.employee.dto;

import com.java.practice.ems.employee.entity.Employee;
import com.java.practice.ems.employee.entity.EmployeeStatus;
import com.java.practice.ems.employee.entity.Department;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ DTO Tests — Testing Record Factories and Mapping Logic ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>WHY TEST DTOs?</h2>
 *
 * <p>
 * DTOs contain mapping logic (Entity → Response) that is critical to the API
 * contract. A broken mapping silently returns wrong data to API consumers.
 * These
 * tests serve as a living contract: if a new field is added to the entity but
 * NOT
 * to the DTO mapper, tests catch the drift immediately.
 * </p>
 *
 * <p>
 * <strong>Clean Test Code principle:</strong> DTOs are pure data
 * transformations
 * — no mocking needed. These are the fastest tests in the suite (<1ms each).
 * </p>
 */
@DisplayName("DTO Mapping Tests")
@SuppressWarnings("null")
class DtoMappingTest {

    private Employee createFullEmployee() {
        Employee employee = Employee.builder()
                .fullName("John Doe")
                .email("john.doe@company.com")
                .phone("+84-123-456-789")
                .status(EmployeeStatus.ACTIVE)
                .build();
        Department dept = new Department();
        ReflectionTestUtils.setField(dept, "id", "dept-1");
        dept.setName("Engineering");
        employee.setDepartment(dept);
        ReflectionTestUtils.setField(employee, "id", "1");
        ReflectionTestUtils.setField(employee, "createdAt", LocalDateTime.of(2024, 1, 15, 9, 0));
        ReflectionTestUtils.setField(employee, "updatedAt", LocalDateTime.of(2024, 6, 15, 14, 30));
        return employee;
    }

    @Nested
    @DisplayName("EmployeeResponse.from()")
    class EmployeeResponseMappingTest {

        @Test
        @DisplayName("should map all entity fields to response DTO")
        void should_MapAllFields_FromEntityToResponse() {
            // ── GIVEN ─────────────────────────────────────────────────────────
            Employee entity = createFullEmployee();

            // ── WHEN ──────────────────────────────────────────────────────────
            EmployeeResponse dto = EmployeeResponse.from(entity);

            assertThat(dto.id()).isEqualTo("1");
            assertThat(dto.fullName()).isEqualTo("John Doe");
            assertThat(dto.email()).isEqualTo("john.doe@company.com");
            assertThat(dto.phone()).isEqualTo("+84-123-456-789");
            assertThat(dto.departmentName()).isEqualTo("Engineering");
            assertThat(dto.baseSalary()).isEqualByComparingTo("5000.00");
            assertThat(dto.joinDate()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(dto.status()).isEqualTo(EmployeeStatus.ACTIVE);
            assertThat(dto.createdAt()).isEqualTo(LocalDateTime.of(2024, 1, 15, 9, 0));
            assertThat(dto.updatedAt()).isEqualTo(LocalDateTime.of(2024, 6, 15, 14, 30));
        }

        @Test
        @DisplayName("should handle null optional fields in entity")
        void should_HandleNullOptionalFields() {
            Employee entity = Employee.builder()
                    .fullName("Jane Smith")
                    .status(EmployeeStatus.ACTIVE)
                    .build();
            ReflectionTestUtils.setField(entity, "id", "2");
            ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.now());

            EmployeeResponse dto = EmployeeResponse.from(entity);

            assertThat(dto.departmentName()).isNull();
            assertThat(dto.baseSalary()).isNull();
            assertThat(dto.joinDate()).isNull();
        }

        @Test
        @DisplayName("should produce Records with correct value-based equality")
        void should_SupportValueEquality() {
            Employee entity = createFullEmployee();
            EmployeeResponse dto1 = EmployeeResponse.from(entity);
            EmployeeResponse dto2 = EmployeeResponse.from(entity);
            assertThat(dto1).isEqualTo(dto2);
        }
    }

    @Nested
    @DisplayName("EmployeeListResponse.from()")
    class EmployeeListResponseMappingTest {

        @Test
        @DisplayName("should map only list-view fields (exclude salary, timestamps)")
        void should_MapOnlyListFields() {
            Employee entity = createFullEmployee();

            EmployeeListResponse dto = EmployeeListResponse.from(entity);

            assertThat(dto.id()).isEqualTo("1");
            assertThat(dto.fullName()).isEqualTo("John Doe");
            assertThat(dto.email()).isEqualTo("john.doe@company.com");
            assertThat(dto.departmentName()).isEqualTo("Engineering");
            assertThat(dto.status()).isEqualTo(EmployeeStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("CreateEmployeeRequest — Compact Constructor Normalization")
    class CreateEmployeeRequestTest {

        @Test
        @DisplayName("should normalize email to lowercase")
        void should_NormalizeEmailToLowercase() {
            CreateEmployeeRequest request = new CreateEmployeeRequest(
                    "John Doe", "JOHN@COMPANY.COM", null, null,
                    null, null, null);
            assertThat(request.email()).isEqualTo("john@company.com");
        }

        @Test
        @DisplayName("should trim whitespace from name fields")
        void should_TrimWhitespaceFromNames() {
            CreateEmployeeRequest request = new CreateEmployeeRequest(
                    "  John Doe  ", "john@company.com", null, null,
                    null, null, null);
            assertThat(request.fullName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("should handle null email gracefully in compact constructor")
        void should_HandleNullEmail() {
            // null email should not throw NPE in compact constructor
            CreateEmployeeRequest request = new CreateEmployeeRequest(
                    "John Doe", null, null, null,
                    null, null, null);
            assertThat(request.email()).isNull();
        }
    }

    @Nested
    @DisplayName("UpdateEmployeeRequest — Compact Constructor Normalization")
    class UpdateEmployeeRequestTest {

        @Test
        @DisplayName("should normalize status to uppercase")
        void should_NormalizeStatusToUppercase() {
            UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                    null, null, null, null,
                    null, null, null, "active");
            assertThat(request.status()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("should normalize email to lowercase in update request")
        void should_NormalizeEmailToLowercase() {
            UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                    null, "JOHN@COMPANY.COM", null, null,
                    null, null, null, null);
            assertThat(request.email()).isEqualTo("john@company.com");
        }

        @Test
        @DisplayName("should allow all fields to be null (valid PATCH with no changes)")
        void should_AllowAllNullFields() {
            UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                    null, null, null, null,
                    null, null, null, null);
            assertThat(request.fullName()).isNull();
            assertThat(request.status()).isNull();
        }
    }
}
