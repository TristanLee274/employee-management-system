package com.java.practice.ems.employee.salary;

import org.springframework.stereotype.Component;

import com.java.practice.ems.employee.entity.Employee;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Concrete Strategy: Daily salary calculation.
 *
 * <p>
 * The {@code baseSalary} is interpreted as a <em>daily rate</em>. Standard
 * Malaysian working calendar: 22 working days per month (52 weeks × 5 days ÷
 * 12).
 * </p>
 *
 * <p>
 * <strong>Demonstrating OCP extension:</strong>
 * This class was added when the company onboarded shift workers paid per day.
 * Zero changes were made to {@link EmployeeService} or any other existing
 * class.
 * Spring automatically picked up this new {@code @Component} via classpath
 * scanning.
 * </p>
 */
@Component
public class DailySalaryCalculator implements SalaryCalculator {

    // Standard working days in a month (varies by country)
    private static final BigDecimal STANDARD_WORK_DAYS = new BigDecimal("22");
    private static final BigDecimal EPF_EMPLOYEE_RATE = new BigDecimal("0.11");
    private static final BigDecimal SOCSO_RATE = new BigDecimal("0.005");

    @Override
    public EmploymentType getSupportedType() {
        return EmploymentType.DAILY;
    }

    /**
     * Calculates net monthly pay for a daily-rated employee.
     *
     * <p>
     * Formula: {@code net = (dailyRate × daysWorked) - EPF - SOCSO}
     * </p>
     *
     * <p>
     * If {@code hoursWorked} is 0 (no timesheet data), assumes a full month of
     * {@code STANDARD_WORK_DAYS} to provide a reasonable estimate.
     * </p>
     *
     * @param employee the employee instance
     * @return net monthly take-home pay
     */
    @Override
    public BigDecimal calculate(Employee employee) {
        BigDecimal baseSalary = employee.getBaseSalary();
        double hoursWorked = employee.getHoursWorked() != null ? employee.getHoursWorked() : 0.0;

        // Derive days worked from hours (standard 8-hour working day)
        BigDecimal daysWorked = hoursWorked > 0
                ? BigDecimal.valueOf(hoursWorked / 8.0).setScale(1, RoundingMode.HALF_UP)
                : STANDARD_WORK_DAYS;

        BigDecimal grossPay = baseSalary.multiply(daysWorked);
        BigDecimal epfDeduction = grossPay.multiply(EPF_EMPLOYEE_RATE);
        BigDecimal socsoDeduction = grossPay.multiply(SOCSO_RATE);

        return grossPay
                .subtract(epfDeduction)
                .subtract(socsoDeduction)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
