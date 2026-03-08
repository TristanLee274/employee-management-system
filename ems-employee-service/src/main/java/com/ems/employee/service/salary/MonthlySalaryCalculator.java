package com.ems.employee.service.salary;

import com.ems.employee.model.enums.EmploymentType;

import org.springframework.stereotype.Component;

import com.ems.employee.model.entity.Employee;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Concrete Strategy: Monthly salary calculation.
 *
 * <p>
 * <strong>Strategy Pattern role — Concrete Strategy:</strong>
 * This class encapsulates the algorithm for monthly salaried employees.
 * The base salary IS the monthly pay — no conversion needed. However,
 * it still applies statutory deductions (CPF, tax withholding) to compute
 * the net take-home amount.
 * </p>
 *
 * <p>
 * <strong>Why @Component?</strong> See {@link EmploymentType#MONTHLY}.
 * Spring registers this as a bean in the application context. The
 * {@link SalaryCalculatorFactory} collects all {@link SalaryCalculator}
 * beans via constructor injection and routes to the right one at runtime —
 * no manual instantiation, no if-else selection.
 * </p>
 */
@Component
public class MonthlySalaryCalculator implements SalaryCalculator {

    // ─── Constants ─────────────────────────────────────────────────────────────
    // Employee Provident Fund rate (employer contribution, varies by country)
    private static final BigDecimal EPF_EMPLOYEE_RATE = new BigDecimal("0.11"); // 11%
    private static final BigDecimal SOCSO_RATE = new BigDecimal("0.005"); // 0.5%

    @Override
    public EmploymentType getSupportedType() {
        return EmploymentType.MONTHLY;
    }

    /**
     * Calculates the net monthly salary for a salaried employee.
     *
     * <p>
     * Formula: {@code net = baseSalary - (EPF_employee + SOCSO)}
     * </p>
     *
     * <p>
     * {@code hoursWorked} is ignored for monthly employees because
     * they receive a fixed rate regardless of hours (unless overtime applies).
     * </p>
     *
     * @param employee the employee to calculate salary for
     * @return net monthly take-home after mandatory deductions
     */
    @Override
    public BigDecimal calculate(Employee employee) {
        BigDecimal baseSalary = employee.getBaseSalary();

        // Mandatory employee statutory deductions
        BigDecimal epfDeduction = baseSalary.multiply(EPF_EMPLOYEE_RATE);
        BigDecimal socsoDeduction = baseSalary.multiply(SOCSO_RATE);

        return baseSalary
                .subtract(epfDeduction)
                .subtract(socsoDeduction)
                .setScale(2, RoundingMode.HALF_UP); // Always round money to 2 decimal places
    }

    /**
     * Override annual salary for 13-month pay structures.
     * Some companies pay a 13th-month bonus. Uncomment to activate:
     * 
     * <pre>
     *   return monthlySalary.multiply(BigDecimal.valueOf(13))
     * </pre>
     */
    @Override
    public BigDecimal annualSalary(BigDecimal monthlySalary) {
        return monthlySalary.multiply(BigDecimal.valueOf(12));
    }
}
