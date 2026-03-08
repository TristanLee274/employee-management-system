package com.ems.employee.service.salary;


import com.ems.employee.model.enums.EmploymentType;

import com.ems.employee.model.entity.Employee;

import java.math.BigDecimal;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ SalaryCalculator — Strategy Pattern Interface ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>STRATEGY DESIGN PATTERN &amp; THE OPEN/CLOSED PRINCIPLE (SOLID)</h2>
 *
 * <p>
 * This interface is the cornerstone of the <strong>Strategy Design
 * Pattern</strong>.
 * It defines a <em>family of algorithms</em> (salary calculation strategies),
 * encapsulates each one, and makes them interchangeable at runtime.
 * </p>
 *
 * <h3>How Strategy Pattern fulfills the Open/Closed Principle</h3>
 *
 * <p>
 * The Open/Closed Principle (OCP) states: <em>"Software entities should be
 * open for extension, but closed for modification."</em>
 * </p>
 *
 * <p>
 * <strong>WITHOUT the Strategy Pattern</strong>, adding a new salary type means
 * editing an existing class with a chain of if-else or switch statements:
 * </p>
 *
 * <pre>{@code
 * // ❌ BAD — violates Open/Closed Principle
 * public BigDecimal calculate(String type, BigDecimal base) {
 *     if (type.equals("HOURLY"))
 *         return base.multiply(HOURS_PER_MONTH);
 *     else if (type.equals("MONTHLY"))
 *         return base;
 *     else if (type.equals("DAILY"))
 *         return base.multiply(WORK_DAYS);
 *     // Adding "COMMISSION" means editing THIS method — modifying closed code!
 * }
 * }</pre>
 *
 * <p>
 * <strong>WITH the Strategy Pattern</strong>, adding a new salary type means
 * CREATE a new class — never touching existing code:
 * </p>
 *
 * <pre>{@code
 * // ✅ GOOD — open for extension, closed for modification
 * public class CommissionSalaryCalculator implements SalaryCalculator {
 *     // New strategy: no existing class modified
 * }
 * }</pre>
 *
 * <h3>The Three Roles in the Strategy Pattern</h3>
 * <ol>
 * <li><strong>Strategy (this interface):</strong> Declares the common algorithm
 * contract.</li>
 * <li><strong>Concrete Strategies:</strong> {@code HourlySalaryCalculator},
 * {@code MonthlySalaryCalculator}, {@code DailySalaryCalculator} — each
 * implements a different calculation algorithm.</li>
 * <li><strong>Context:</strong> {@code EmployeeService} — holds a reference to
 * a
 * {@code SalaryCalculator} and delegates the calculation. It doesn't know
 * (nor care) which concrete strategy is used.</li>
 * </ol>
 *
 * <h3>DEPENDENCY INVERSION PRINCIPLE (SOLID)</h3>
 *
 * <p>
 * The {@code EmployeeService} depends on THIS INTERFACE — a high-level
 * abstraction —
 * not on any concrete calculator class. This is the Dependency Inversion
 * Principle:
 * </p>
 *
 * <pre>
 *   EmployeeService  ──depends on──►  SalaryCalculator (interface)
 *                                            ▲
 *                        ┌───────────────────┤
 *                        │                   │
 *             HourlySalaryCalculator   MonthlySalaryCalculator
 *             (low-level detail)       (low-level detail)
 * </pre>
 *
 * <p>
 * Benefits:
 * </p>
 * <ul>
 * <li>The service is decoupled from the implementation details of any
 * calculator</li>
 * <li>In tests, you inject a mock:
 * {@code when(calculator.calculate(...)).thenReturn(...)}</li>
 * <li>At runtime, Spring IoC selects and injects the right implementation based
 * on the
 * employee's employment type</li>
 * </ul>
 */
public interface SalaryCalculator {

    /**
     * Computes the net monthly take-home amount for an employee
     * given their base salary rate.
     *
     * <p>
     * Each strategy interprets {@code baseSalary} differently:
     * </p>
     * <ul>
     * <li>Monthly: {@code baseSalary} IS the monthly amount → no conversion
     * needed</li>
     * <li>Hourly: {@code baseSalary} is the hourly rate → multiply by
     * hours/month</li>
     * <li>Daily: {@code baseSalary} is the daily rate → multiply by working
     * days/month</li>
     * </ul>
     *
     * @param employee the employee instance to calculate salary for (never null)
     * @return the computed monthly salary amount, never null
     */
    BigDecimal calculate(Employee employee);

    /**
     * Returns the employment type identifier this strategy handles.
     * Used by the {@link SalaryCalculatorFactory} to select the correct strategy.
     *
     * @return the {@link EmploymentType} this strategy applies to
     */
    EmploymentType getSupportedType();

    /**
     * Computes the annual gross salary from the monthly figure.
     * <p>
     * Default implementation multiplies the monthly salary by 12.
     * Concrete strategies can override this for non-standard annual cycles
     * (e.g., 13-month pay structures common in some countries).
     *
     * @param monthlySalary the computed monthly salary
     * @return the annual gross salary
     */
    default BigDecimal annualSalary(BigDecimal monthlySalary) {
        return monthlySalary.multiply(BigDecimal.valueOf(12));
    }
}
