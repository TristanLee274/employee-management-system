package com.java.practice.ems.employee.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ Employee Entity — JPA Domain Model ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>WHY A JPA ENTITY IS SEPARATE FROM DTOs (Single Responsibility
 * Principle)</h2>
 *
 * <p>
 * This class has ONE responsibility: <strong>represent the "employees" table as
 * a Java
 * object and manage its lifecycle within the JPA/Hibernate persistence
 * context.</strong>
 * </p>
 *
 * <p>
 * We deliberately do NOT expose this entity directly to the REST API because:
 * </p>
 * <ul>
 * <li><strong>Security:</strong> Internal fields like {@code passwordHash} or
 * audit timestamps
 * should never leak to API consumers. DTOs act as a "view filter".</li>
 * <li><strong>Stability:</strong> Database schema changes (adding a column)
 * shouldn't break
 * the API contract. The DTO remains stable while the entity evolves.</li>
 * <li><strong>Flexibility:</strong> A single entity can map to multiple DTOs —
 * {@code EmployeeResponse} (full details), {@code EmployeeListItem} (summary),
 * {@code EmployeeExport} (CSV format) — each serving a different consumer.</li>
 * <li><strong>Validation separation:</strong> Entity constraints
 * ({@code @Column(nullable=false)})
 * enforce database integrity. DTO validation ({@code @NotBlank}) enforces API
 * contracts.
 * These are different concerns that change for different reasons.</li>
 * </ul>
 *
 * <h2>WHY PostgreSQL FOR EMPLOYEE DATA</h2>
 *
 * <p>
 * A relational database (PostgreSQL) is the right choice for employee data
 * because:
 * </p>
 * <ul>
 * <li><strong>ACID compliance:</strong> Salary updates, department transfers,
 * and role changes
 * MUST be atomic. A failed salary update should never leave partial data.</li>
 * <li><strong>Referential integrity:</strong> Foreign keys enforce that an
 * employee's department
 * actually exists. NoSQL databases cannot guarantee this at the database
 * level.</li>
 * <li><strong>Complex queries:</strong> HR reporting requires JOINs, GROUP BY,
 * window functions
 * (e.g., "average salary by department", "employees hired last quarter").</li>
 * <li><strong>Data consistency:</strong> Employee records are the "source of
 * truth" for payroll,
 * benefits, and compliance. Eventual consistency (typical in NoSQL) is
 * unacceptable here.</li>
 * <li><strong>PostgreSQL-specific advantages:</strong> JSONB for flexible
 * metadata, full-text
 * search for employee directories, row-level security for multi-tenant
 * setups.</li>
 * </ul>
 *
 * <h2>LOMBOK ANNOTATIONS EXPLAINED</h2>
 *
 * <p>
 * We use Lombok to eliminate boilerplate while keeping the entity focused on
 * its domain:
 * </p>
 * <ul>
 * <li>{@code @Getter/@Setter} — Generates accessor methods. We avoid
 * {@code @Data} on entities
 * because its {@code equals/hashCode} implementation uses all fields, which
 * breaks JPA
 * identity semantics (entities should use ID-based equality).</li>
 * <li>{@code @Builder} — Fluent construction:
 * {@code Employee.builder().firstName("John").build()}</li>
 * <li>{@code @NoArgsConstructor} — Required by JPA/Hibernate for proxy
 * creation.</li>
 * <li>{@code @AllArgsConstructor} — Required by {@code @Builder}.</li>
 * </ul>
 */
@Entity
@Table(name = "employees",
        // Database-level constraints complement JPA validation:
        // • Unique email enforced at DB level prevents race conditions that
        // application-level checks alone cannot catch (two concurrent requests).
        // • Indexes on frequently queried columns (email, department) accelerate
        // lookups without changing application code (Open/Closed Principle).
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employees_email", columnNames = "email")
        }, indexes = {
                @Index(name = "idx_employees_department", columnList = "department"),
                @Index(name = "idx_employees_status", columnList = "status"),
                @Index(name = "idx_employees_last_name", columnList = "last_name")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMARY KEY
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * PostgreSQL IDENTITY column strategy:
     * <p>
     * We use {@code GenerationType.IDENTITY} because:
     * <ul>
     * <li>Maps directly to PostgreSQL's {@code BIGSERIAL} /
     * {@code GENERATED ALWAYS AS IDENTITY}</li>
     * <li>No sequence table overhead (unlike {@code TABLE} strategy)</li>
     * <li>Auto-incremented by the database — no round-trip to fetch the next
     * value</li>
     * <li>Note: Disables JDBC batch inserts for this entity. If batch inserts are
     * critical, switch to {@code SEQUENCE} strategy with
     * {@code @SequenceGenerator}.</li>
     * </ul>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ═══════════════════════════════════════════════════════════════════════════
    // PERSONAL INFORMATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * Email is the natural business key for employees.
     * <p>
     * The unique constraint is enforced at BOTH levels:
     * <ul>
     * <li>JPA level: {@code unique = true} generates DDL constraint</li>
     * <li>Database level: {@code uk_employees_email} constraint (defined
     * in @Table)</li>
     * </ul>
     * Double enforcement because JPA validation happens before the SQL executes,
     * but concurrent requests can slip through — the DB constraint is the final
     * guard.
     */
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    // ═══════════════════════════════════════════════════════════════════════════
    // EMPLOYMENT DETAILS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Department as a String field (denormalized).
     * <p>
     * In a production system, this would be a {@code @ManyToOne} relationship
     * to a {@code Department} entity. We use a String here for simplicity:
     * 
     * <pre>
     * // Future: @ManyToOne(fetch = FetchType.LAZY)
     * // @JoinColumn(name = "department_id")
     * // private Department department;
     * </pre>
     */
    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "job_title", length = 150)
    private String jobTitle;

    /**
     * Salary stored as {@code BigDecimal} — NEVER use {@code double} for money.
     * <p>
     * {@code double} has floating-point precision issues:
     * {@code 0.1 + 0.2 = 0.30000000000000004} in IEEE 754.
     * {@code BigDecimal} provides exact arithmetic, critical for payroll
     * calculations.
     * <p>
     * PostgreSQL maps this to {@code NUMERIC(12,2)} — 12 total digits, 2 decimal
     * places.
     * Supports salaries up to 9,999,999,999.99
     */
    @Column(name = "salary", precision = 12, scale = 2)
    private BigDecimal salary;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    /**
     * Employment status using Java enum mapped to PostgreSQL VARCHAR.
     * <p>
     * {@code EnumType.STRING} stores the enum NAME (e.g., "ACTIVE") in the
     * database,
     * not the ordinal (e.g., 0). This is crucial because:
     * <ul>
     * <li>Adding a new enum value (e.g., {@code ON_LEAVE}) won't shift
     * ordinals</li>
     * <li>Database queries are human-readable: {@code WHERE status = 'ACTIVE'}</li>
     * <li>Reordering enum constants won't corrupt existing data</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    // ═══════════════════════════════════════════════════════════════════════════
    // AUDIT FIELDS
    // ═══════════════════════════════════════════════════════════════════════════
    //
    // Audit timestamps follow the Single Responsibility Principle:
    // The Entity owns its lifecycle metadata. The application code never
    // manually sets these — Hibernate's @CreationTimestamp and @UpdateTimestamp
    // handle it automatically, reducing human error.
    //

    /**
     * Automatically set by Hibernate when the entity is first persisted.
     * {@code updatable = false} prevents accidental overwrites on UPDATE.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Automatically updated by Hibernate on every UPDATE operation.
     * Useful for optimistic locking and "last modified" displays.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ═══════════════════════════════════════════════════════════════════════════
    // EQUALS & HASHCODE — JPA Entity Identity Contract
    // ═══════════════════════════════════════════════════════════════════════════
    //
    // We override equals/hashCode manually instead of using Lombok's @Data because:
    //
    // JPA entities have identity semantics based on their primary key (id), not
    // on all fields. Two Employee objects with the same id represent the SAME
    // database row, regardless of whether other fields differ (e.g., after an
    // update in a different transaction).
    //
    // Using @Data generates equals/hashCode from ALL fields, which breaks:
    // • HashSet/HashMap behavior when entities are modified after insertion
    // • Hibernate's dirty checking and first-level cache
    //

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Employee employee = (Employee) o;
        // Only compare by id — new (unsaved) entities are never equal
        return id != null && id.equals(employee.id);
    }

    @Override
    public int hashCode() {
        // Constant hash code for new entities: ensures consistent behavior
        // in collections before and after the entity is persisted.
        // After persist, id is assigned, but hashCode must remain stable.
        // See:
        // https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        // Exclude sensitive data (salary) and lazy associations from toString
        // to prevent accidental logging of PII and LazyInitializationException.
        return "Employee{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", department='" + department + '\'' +
                ", status=" + status +
                '}';
    }
}
