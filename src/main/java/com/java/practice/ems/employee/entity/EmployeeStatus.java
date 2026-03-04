package com.java.practice.ems.employee.entity;

/**
 * Employment status enum representing the lifecycle states of an employee.
 *
 * <p>
 * Using a Java enum (mapped to PostgreSQL VARCHAR via
 * {@code @Enumerated(EnumType.STRING)})
 * provides type safety at compile time — you cannot assign an invalid status
 * like "ACTVE"
 * (a typo) because the compiler rejects it. This is safer than using raw
 * strings.
 * </p>
 *
 * <p>
 * <strong>Design Decision:</strong> We keep the enum inside the {@code entity}
 * package
 * because it's an intrinsic part of the domain model. DTOs reference this enum
 * by name
 * (as a String in responses, or parsed from request strings) to maintain the
 * decoupling
 * between persistence and API layers.
 * </p>
 */
public enum EmployeeStatus {

    /**
     * Currently employed and active in the system.
     */
    ACTIVE,

    /**
     * Employment has ended (resigned or contract completed).
     * Records are retained for historical/compliance purposes.
     */
    INACTIVE,

    /**
     * Approved leave of absence (maternity, medical, sabbatical).
     * Employee retains their position and benefits.
     */
    ON_LEAVE,

    /**
     * Recently hired, undergoing onboarding and probation period.
     * May have restricted system access until probation completes.
     */
    PROBATION,

    /**
     * Employment terminated by the organization.
     * Subject to different compliance and reporting requirements than INACTIVE.
     */
    TERMINATED
}
