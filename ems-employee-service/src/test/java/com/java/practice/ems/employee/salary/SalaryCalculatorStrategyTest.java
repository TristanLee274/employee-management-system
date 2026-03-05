package com.java.practice.ems.employee.salary;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import com.java.practice.ems.employee.entity.Employee;

import static org.assertj.core.api.Assertions.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ Salary Strategy Tests — Testing Each Concrete Strategy in Isolation ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>WHY TEST EACH STRATEGY SEPARATELY?</h2>
 *
 * <p>
 * The Strategy Pattern decomposes salary calculation into independent units.
 * Each unit (concrete strategy) should be tested in isolation, because:
 * </p>
 * <ul>
 * <li><strong>Single Responsibility:</strong> Each calculator has ONE
 * algorithm.
 * Testing it alone ensures correctness of that specific algorithm.</li>
 * <li><strong>Fast feedback:</strong> If a commission calculation breaks, only
 * CommissionSalaryCalculator tests fail — you know EXACTLY where the bug
 * is.</li>
 * <li><strong>No mocking needed:</strong> These are pure functions — BigDecimal
 * in,
 * BigDecimal out. No database, no Kafka, no Spring context required.</li>
 * </ul>
 *
 * <h2>PARAMETERIZED TESTS (JUnit 5)</h2>
 *
 * <p>
 * We use {@code @ParameterizedTest} with {@code @CsvSource} to test multiple
 * input-output combinations without duplicating test methods. This is the TDD
 * equivalent of "one test method, many scenarios" — clean, DRY, comprehensive.
 * </p>
 */
class SalaryCalculatorStrategyTest {

    // ═══════════════════════════════════════════════════════════════════════════
    // MONTHLY SALARY CALCULATOR
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("MonthlySalaryCalculator")
    class MonthlySalaryCalculatorTest {

        private final MonthlySalaryCalculator calculator = new MonthlySalaryCalculator();

        @Test
        @DisplayName("should return EmploymentType.MONTHLY as supported type")
        void should_ReturnMonthlyType() {
            assertThat(calculator.getSupportedType()).isEqualTo(EmploymentType.MONTHLY);
        }

        @Test
        @DisplayName("should calculate net salary with EPF (11%) and SOCSO (0.5%) deductions")
        void should_CalculateNetSalary_WithStatutoryDeductions() {
            // GIVEN: base salary = 5000.00
            BigDecimal baseSalary = new BigDecimal("5000.00");

            // WHEN: calculate net monthly salary
            Employee employee = Employee.builder().baseSalary(baseSalary).hoursWorked(0.0).build();
            BigDecimal result = calculator.calculate(employee);

            // THEN: net = 5000 - (5000 * 0.11) - (5000 * 0.005)
            // = 5000 - 550 - 25 = 4425.00
            assertThat(result).isEqualByComparingTo("4425.00");
        }

        @ParameterizedTest(name = "base={0} → net={1}")
        @CsvSource({
                "1000.00,  885.00", // 1000 - 110 - 5
                "3000.00,  2655.00", // 3000 - 330 - 15
                "10000.00, 8850.00", // 10000 - 1100 - 50
                "0.01,     0.01" // edge case: tiny salary
        })
        @DisplayName("should calculate correctly for various salaries")
        void should_CalculateCorrectly_ForVariousSalaries(String base, String expected) {
            Employee employee = Employee.builder().baseSalary(new BigDecimal(base)).hoursWorked(0.0).build();
            BigDecimal result = calculator.calculate(employee);
            assertThat(result).isEqualByComparingTo(expected);
        }

        @Test
        @DisplayName("should ignore hoursWorked parameter for monthly employees")
        void should_IgnoreHoursWorked() {
            BigDecimal salary = new BigDecimal("5000.00");
            // Hours worked should NOT affect monthly salary
            Employee emp1 = Employee.builder().baseSalary(salary).hoursWorked(0.0).build();
            Employee emp2 = Employee.builder().baseSalary(salary).hoursWorked(200.0).build();

            BigDecimal result1 = calculator.calculate(emp1);
            BigDecimal result2 = calculator.calculate(emp2);
            assertThat(result1).isEqualByComparingTo(result2);
        }

        @Test
        @DisplayName("should calculate annual salary as 12 × monthly")
        void should_CalculateAnnualSalary() {
            BigDecimal monthly = new BigDecimal("4425.00");
            BigDecimal annual = calculator.annualSalary(monthly);
            assertThat(annual).isEqualByComparingTo("53100.00");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HOURLY SALARY CALCULATOR
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("HourlySalaryCalculator")
    class HourlySalaryCalculatorTest {

        private final HourlySalaryCalculator calculator = new HourlySalaryCalculator();

        @Test
        @DisplayName("should return EmploymentType.HOURLY as supported type")
        void should_ReturnHourlyType() {
            assertThat(calculator.getSupportedType()).isEqualTo(EmploymentType.HOURLY);
        }

        @Test
        @DisplayName("should calculate regular pay without overtime (≤160 hours)")
        void should_CalculateRegularPay_WhenNoOvertime() {
            // GIVEN: hourly rate = 50, hours = 160 (exactly threshold)
            BigDecimal hourlyRate = new BigDecimal("50.00");

            // WHEN
            Employee employee = Employee.builder().baseSalary(hourlyRate).hoursWorked(160.0).build();
            BigDecimal result = calculator.calculate(employee);

            // THEN: gross = 50 * 160 = 8000
            // net = 8000 - (8000 * 0.11) - (8000 * 0.005) = 8000 - 880 - 40 = 7080
            assertThat(result).isEqualByComparingTo("7080.00");
        }

        @Test
        @DisplayName("should include 1.5× overtime pay for hours > 160")
        void should_IncludeOvertimePay_WhenExceedsThreshold() {
            // GIVEN: hourly rate = 50, hours = 180 (20 overtime hours)
            BigDecimal hourlyRate = new BigDecimal("50.00");

            // WHEN
            Employee employee = Employee.builder().baseSalary(hourlyRate).hoursWorked(180.0).build();
            BigDecimal result = calculator.calculate(employee);

            // THEN: regular = 50 * 160 = 8000
            // overtime = 50 * 1.5 * 20 = 1500
            // gross = 8000 + 1500 = 9500
            // net = 9500 - (9500 * 0.11) - (9500 * 0.005) = 9500 - 1045 - 47.5 = 8407.50
            assertThat(result).isEqualByComparingTo("8407.50");
        }

        @Test
        @DisplayName("should handle zero hours gracefully")
        void should_ReturnZero_WhenZeroHoursWorked() {
            Employee employee = Employee.builder().baseSalary(new BigDecimal("50.00")).hoursWorked(0.0).build();
            BigDecimal result = calculator.calculate(employee);
            assertThat(result).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("should use default annualSalary from interface (12× monthly)")
        void should_UseDefaultAnnualSalary() {
            BigDecimal monthly = new BigDecimal("7080.00");
            // HourlySalaryCalculator does NOT override annualSalary()
            // → uses the default interface method: monthly × 12
            assertThat(calculator.annualSalary(monthly)).isEqualByComparingTo("84960.00");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DAILY SALARY CALCULATOR
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DailySalaryCalculator")
    class DailySalaryCalculatorTest {

        private final DailySalaryCalculator calculator = new DailySalaryCalculator();

        @Test
        @DisplayName("should return EmploymentType.DAILY as supported type")
        void should_ReturnDailyType() {
            assertThat(calculator.getSupportedType()).isEqualTo(EmploymentType.DAILY);
        }

        @Test
        @DisplayName("should assume 22 working days when hoursWorked is 0")
        void should_AssumeFullMonth_WhenHoursIsZero() {
            // GIVEN: daily rate = 200, hours = 0 → assumes 22 days
            BigDecimal dailyRate = new BigDecimal("200.00");

            // WHEN
            Employee employee = Employee.builder().baseSalary(dailyRate).hoursWorked(0.0).build();
            BigDecimal result = calculator.calculate(employee);

            // THEN: gross = 200 * 22 = 4400
            // net = 4400 - (4400 * 0.11) - (4400 * 0.005) = 4400 - 484 - 22 = 3894
            assertThat(result).isEqualByComparingTo("3894.00");
        }

        @Test
        @DisplayName("should derive days from hours (hours / 8) when hoursWorked > 0")
        void should_DeriveDaysFromHours_WhenHoursProvided() {
            // GIVEN: daily rate = 200, hours = 80 → 80/8 = 10 days
            BigDecimal dailyRate = new BigDecimal("200.00");

            // WHEN
            Employee employee = Employee.builder().baseSalary(dailyRate).hoursWorked(80.0).build();
            BigDecimal result = calculator.calculate(employee);

            // THEN: gross = 200 * 10.0 = 2000
            // net = 2000 - (2000 * 0.11) - (2000 * 0.005) = 2000 - 220 - 10 = 1770
            assertThat(result).isEqualByComparingTo("1770.00");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COMMISSION SALARY CALCULATOR
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("CommissionSalaryCalculator")
    class CommissionSalaryCalculatorTest {

        private final CommissionSalaryCalculator calculator = new CommissionSalaryCalculator();

        @Test
        @DisplayName("should return EmploymentType.COMMISSION as supported type")
        void should_ReturnCommissionType() {
            assertThat(calculator.getSupportedType()).isEqualTo(EmploymentType.COMMISSION);
        }

        @Test
        @DisplayName("should calculate base + commission at 80% achievement")
        void should_CalculateWithCommission_At80PercentAchievement() {
            // GIVEN: base = 3000, achievement = 80%
            BigDecimal base = new BigDecimal("3000.00");

            // WHEN
            Employee employee = Employee.builder().baseSalary(base).hoursWorked(80.0).build();
            BigDecimal result = calculator.calculate(employee);

            // THEN: commission = 3000 * 0.20 * 0.80 = 480
            // gross = 3000 + 480 = 3480
            // net = 3480 - (3480 * 0.11) - (3480 * 0.005) = 3480 - 382.8 - 17.4 = 3079.80
            assertThat(result).isEqualByComparingTo("3079.80");
        }

        @Test
        @DisplayName("should cap achievement at 100%")
        void should_CapAchievementAt100Percent() {
            BigDecimal base = new BigDecimal("3000.00");
            // 150% achievement should be capped to 100%
            Employee emp1 = Employee.builder().baseSalary(base).hoursWorked(150.0).build();
            Employee emp2 = Employee.builder().baseSalary(base).hoursWorked(100.0).build();

            BigDecimal resultCapped = calculator.calculate(emp1);
            BigDecimal resultMax = calculator.calculate(emp2);
            assertThat(resultCapped).isEqualByComparingTo(resultMax);
        }

        @Test
        @DisplayName("should floor achievement at 0%")
        void should_FloorAchievementAtZeroPercent() {
            BigDecimal base = new BigDecimal("3000.00");
            // -10% achievement should be treated as 0%
            Employee emp1 = Employee.builder().baseSalary(base).hoursWorked(-10.0).build();
            Employee emp2 = Employee.builder().baseSalary(base).hoursWorked(0.0).build();

            BigDecimal resultNegative = calculator.calculate(emp1);
            BigDecimal resultZero = calculator.calculate(emp2);
            assertThat(resultNegative).isEqualByComparingTo(resultZero);
        }

        @Test
        @DisplayName("should return net base salary when achievement is 0%")
        void should_ReturnNetBaseSalary_WhenZeroAchievement() {
            BigDecimal base = new BigDecimal("3000.00");
            Employee employee = Employee.builder().baseSalary(base).hoursWorked(0.0).build();
            BigDecimal result = calculator.calculate(employee);
            // commission = 0, gross = 3000, net = 3000 - 330 - 15 = 2655
            assertThat(result).isEqualByComparingTo("2655.00");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SALARY CALCULATOR FACTORY
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("SalaryCalculatorFactory")
    class SalaryCalculatorFactoryTest {

        private SalaryCalculatorFactory factory;

        @BeforeEach
        void setUp() {
            // Manually construct the factory with all strategy beans.
            // In production, Spring injects these automatically via @Component scanning.
            // In tests, we wire them manually to test the factory's routing logic.
            List<SalaryCalculator> calculators = List.of(
                    new MonthlySalaryCalculator(),
                    new HourlySalaryCalculator(),
                    new DailySalaryCalculator(),
                    new CommissionSalaryCalculator());
            factory = new SalaryCalculatorFactory(calculators);
        }

        @ParameterizedTest(name = "type ''{0}'' → {1}")
        @CsvSource({
                "MONTHLY,    MONTHLY",
                "HOURLY,     HOURLY",
                "DAILY,      DAILY",
                "COMMISSION, COMMISSION"
        })
        @DisplayName("should resolve correct calculator for known types")
        void should_ResolveCorrectCalculator_ForKnownTypes(String typeStr, String expectedType) {
            SalaryCalculator calculator = factory.getCalculator(typeStr);
            assertThat(calculator).isNotNull();
            assertThat(calculator.getSupportedType().name()).isEqualTo(expectedType);
        }

        @Test
        @DisplayName("should default to MONTHLY when type string is null")
        void should_DefaultToMonthly_WhenTypeIsNull() {
            SalaryCalculator calculator = factory.getCalculator((String) null);
            assertThat(calculator.getSupportedType()).isEqualTo(EmploymentType.MONTHLY);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for unknown type string")
        void should_ThrowException_ForUnknownType() {
            assertThatThrownBy(() -> factory.getCalculator("FREELANCE"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown employment type");
        }

        @Test
        @DisplayName("should resolve calculator via EmploymentType enum overload")
        void should_ResolveViaEnumOverload() {
            SalaryCalculator calculator = factory.getCalculator(EmploymentType.HOURLY);
            assertThat(calculator).isNotNull();
            assertThat(calculator.getSupportedType()).isEqualTo(EmploymentType.HOURLY);
        }
    }
}
