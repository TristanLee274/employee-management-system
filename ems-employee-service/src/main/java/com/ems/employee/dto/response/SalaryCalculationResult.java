package com.ems.employee.dto.response;

import java.math.BigDecimal;

/**
 * Result DTO for salary calculation operations.
 *
 * <p>
 * <strong>Java 21 Record as a Result Carrier:</strong>
 * The salary calculation produces multiple values (monthly, annual, base).
 * Rather than returning a raw {@code BigDecimal} and forcing the caller to
 * make additional lookups, this Record bundles all related results immutably.
 * Records are ideal for this "return multiple values" scenario — no mutable
 * result objects, no parallel arrays, no maps with magic string keys.
 * </p>
 *
 * @param employeeId       the employee whose salary was calculated
 * @param employeeName     full name for display purposes
 * @param employmentType   the salary strategy used (e.g., "HOURLY")
 * @param baseSalary       the configured base rate
 * @param netMonthlySalary calculated net take-home per month
 * @param netAnnualSalary  calculated net take-home per year
 * @param hoursWorked      input hours that influenced the calculation
 */
public record SalaryCalculationResult(
                String employeeId,
                String employeeName,
                String employmentType,
                BigDecimal baseSalary,
                BigDecimal netMonthlySalary,
                BigDecimal netAnnualSalary,
                double hoursWorked) {
}
