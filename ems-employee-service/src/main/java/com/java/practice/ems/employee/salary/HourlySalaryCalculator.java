package com.java.practice.ems.employee.salary;

import org.springframework.stereotype.Component;

import com.java.practice.ems.employee.entity.Employee;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Concrete Strategy: Hourly salary calculation.
 *
 * <p>
 * <strong>Strategy Pattern role — Concrete Strategy:</strong>
 * This class encapsulates the algorithm for hourly-paid employees.
 * The {@code baseSalary} is interpreted as an <em>hourly rate</em>. Actual
 * hours
 * worked (from timesheets) are multiplied to derive the gross monthly pay.
 * </p>
 *
 * <p>
 * <strong>Open/Closed Principle in action:</strong>
 * When the business introduced hourly employees, a NEW class was created here.
 * The {@link EmployeeService}, {@link SalaryCalculatorFactory}, and ALL other
 * classes remained completely untouched. That's OCP — open for extension by
 * adding
 * a new file, closed to modification of existing files.
 * </p>
 */
@Component
public class HourlySalaryCalculator implements SalaryCalculator {

    // Overtime threshold: hours before 1.5× rate kicks in (standard 40h/wk × 4wks)
    private static final double OVERTIME_THRESHOLD = 160.0;
    private static final BigDecimal OVERTIME_MULTIPLIER = new BigDecimal("1.5"); // 1.5× rate
    private static final BigDecimal EPF_EMPLOYEE_RATE = new BigDecimal("0.11");
    private static final BigDecimal SOCSO_RATE = new BigDecimal("0.005");

    @Override
    public EmploymentType getSupportedType() {
        return EmploymentType.HOURLY;
    }

    /**
     * Calculates net monthly pay for an hourly employee, including overtime.
     *
     * <p>
     * Algorithm:
     * </p>
     * <ol>
     * <li>Split hours into regular (≤ 160h) and overtime (> 160h)</li>
     * <li>Regular pay = regularHours × hourlyRate</li>
     * <li>Overtime pay = overtimeHours × hourlyRate × 1.5</li>
     * <li>Gross = regular + overtime</li>
     * <li>Net = gross − EPF − SOCSO</li>
     * </ol>
     *
     * @param employee the employee instance containing hourly pay rate and actual
     *                 hours worked
     * @return net monthly take-home pay
     */
    @Override
    public BigDecimal calculate(Employee employee) {
        BigDecimal baseSalary = employee.getBaseSalary();
        double hoursWorked = employee.getHoursWorked() != null ? employee.getHoursWorked() : 0.0;

        // ── Step 1: Separate regular and overtime hours ──────────────────────
        double regularHours = Math.min(hoursWorked, OVERTIME_THRESHOLD);
        double overtimeHours = Math.max(0, hoursWorked - OVERTIME_THRESHOLD);

        // ── Step 2: Calculate regular and overtime gross pay ─────────────────
        BigDecimal regularPay = baseSalary.multiply(BigDecimal.valueOf(regularHours));
        BigDecimal overtimePay = baseSalary
                .multiply(OVERTIME_MULTIPLIER)
                .multiply(BigDecimal.valueOf(overtimeHours));

        BigDecimal grossPay = regularPay.add(overtimePay);

        // ── Step 3: Apply statutory deductions ───────────────────────────────
        BigDecimal epfDeduction = grossPay.multiply(EPF_EMPLOYEE_RATE);
        BigDecimal socsoDeduction = grossPay.multiply(SOCSO_RATE);

        return grossPay
                .subtract(epfDeduction)
                .subtract(socsoDeduction)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
