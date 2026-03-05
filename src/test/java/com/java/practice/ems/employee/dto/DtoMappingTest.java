package com.java.practice.ems.employee.dto;

import com.java.practice.ems.employee.entity.Employee;
import com.java.practice.ems.employee.entity.EmployeeStatus;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
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
class DtoMappingTest {

    private Employee createFullEmployee() {
        return Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@company.com")
                .phone("+84-123-456-789")
                .department("Engineering")
                .jobTitle("Senior Developer")
                .salary(new BigDecimal("5000.00"))
                .hireDate(LocalDate.of(2024, 1, 15))
                .status(EmployeeStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2024, 1, 15, 9, 0))
                .updatedAt(LocalDateTime.of(2024, 6, 15, 14, 30))
                .build();
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

            // ── THEN ──────────────────────────────────────────────────────────
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.firstName()).isEqualTo("John");
            assertThat(dto.lastName()).isEqualTo("Doe");
            assertThat(dto.email()).isEqualTo("john.doe@company.com");
            assertThat(dto.phone()).isEqualTo("+84-123-456-789");
            assertThat(dto.department()).isEqualTo("Engineering");
            assertThat(dto.jobTitle()).isEqualTo("Senior Developer");
            assertThat(dto.salary()).isEqualByComparingTo("5000.00");
            assertThat(dto.hireDate()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(dto.status()).isEqualTo(EmployeeStatus.ACTIVE);
            assertThat(dto.createdAt()).isEqualTo(LocalDateTime.of(2024, 1, 15, 9, 0));
            assertThat(dto.updatedAt()).isEqualTo(LocalDateTime.of(2024, 6, 15, 14, 30));
        }

        @Test
        @DisplayName("should handle null optional fields in entity")
        void should_HandleNullOptionalFields() {
            Employee entity = Employee.builder()
                    .id(2L)
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane@company.com")
                    .status(EmployeeStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();

            EmployeeResponse dto = EmployeeResponse.from(entity);

            assertThat(dto.phone()).isNull();
            assertThat(dto.department()).isNull();
            assertThat(dto.salary()).isNull();
            assertThat(dto.hireDate()).isNull();
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

            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.firstName()).isEqualTo("John");
            assertThat(dto.lastName()).isEqualTo("Doe");
            assertThat(dto.email()).isEqualTo("john.doe@company.com");
            assertThat(dto.department()).isEqualTo("Engineering");
            assertThat(dto.jobTitle()).isEqualTo("Senior Developer");
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
                    "John", "Doe", "JOHN@COMPANY.COM", null,
                    null, null, null, null);
            assertThat(request.email()).isEqualTo("john@company.com");
        }

        @Test
        @DisplayName("should trim whitespace from name fields")
        void should_TrimWhitespaceFromNames() {
            CreateEmployeeRequest request = new CreateEmployeeRequest(
                    "  John  ", "  Doe  ", "john@company.com", null,
                    null, null, null, null);
            assertThat(request.firstName()).isEqualTo("John");
            assertThat(request.lastName()).isEqualTo("Doe");
        }

        @Test
        @DisplayName("should handle null email gracefully in compact constructor")
        void should_HandleNullEmail() {
            // null email should not throw NPE in compact constructor
            CreateEmployeeRequest request = new CreateEmployeeRequest(
                    "John", "Doe", null, null,
                    null, null, null, null);
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
                    null, null, null, null, "active");
            assertThat(request.status()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("should normalize email to lowercase in update request")
        void should_NormalizeEmailToLowercase() {
            UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                    null, null, "JOHN@COMPANY.COM", null,
                    null, null, null, null, null);
            assertThat(request.email()).isEqualTo("john@company.com");
        }

        @Test
        @DisplayName("should allow all fields to be null (valid PATCH with no changes)")
        void should_AllowAllNullFields() {
            UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                    null, null, null, null,
                    null, null, null, null, null);
            assertThat(request.firstName()).isNull();
            assertThat(request.status()).isNull();
        }
    }
}
