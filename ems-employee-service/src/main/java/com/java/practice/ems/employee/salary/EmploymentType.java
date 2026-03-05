package com.java.practice.ems.employee.salary;

/**
 * Employment type enum used to select the correct salary calculation strategy.
 *
 * <p>
 * Each constant maps to one concrete {@link SalaryCalculator} implementation.
 * This is used by {@link SalaryCalculatorFactory} to resolve the right strategy
 * at runtime without any if-else chains.
 * </p>
 *
 * <p>
 * <strong>Java 21 Pattern Matching for switch</strong> uses this enum directly
 * in exhaustive switch expressions, so the compiler enforces that every type is
 * handled — you cannot forget a case.
 * </p>
 */
public enum EmploymentType {

    /**
     * Fixed monthly salary. Most common for salaried employees.
     * Rate unit: currency per month.
     */
    MONTHLY,

    /**
     * Pay is calculated per hour worked.
     * Rate unit: currency per hour.
     * Requires actual hours worked for accurate calculation.
     */
    HOURLY,

    /**
     * Pay is calculated per working day.
     * Rate unit: currency per day.
     * Standard month = 22 working days.
     */
    DAILY,

    /**
     * Contract-based pay — typically project-deliverable milestones.
     * Rate unit: fixed contract amount per period.
     */
    CONTRACT,

    /**
     * Base salary plus commission on sales/performance targets.
     * Rate unit: base currency per month + commission percentage.
     */
    COMMISSION
}
