package com.ems.employee.repository;

import java.math.BigDecimal;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ DepartmentStatsProjection — Interface-based Projection ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 * Enables mapping Native Queries directly to an interface without an
 * intermediate class.
 */
public interface DepartmentStatsProjection {
    String getDepartmentName();

    Long getEmployeeCount();

    BigDecimal getAvgSalary();
}
