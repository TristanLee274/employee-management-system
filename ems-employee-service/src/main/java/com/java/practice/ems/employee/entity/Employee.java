package com.java.practice.ems.employee.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ Employee Entity — JPA Domain Model ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <p>
 * Representing the "employees" table as a Java object and managing its
 * lifecycle
 * within the JPA/Hibernate persistence context. We deliberately do NOT expose
 * this
 * entity directly to the REST API, keeping concerns separated.
 * </p>
 */
@Entity
@Table(name = "employees", uniqueConstraints = {
        @UniqueConstraint(name = "uk_employees_email", columnNames = "email")
}, indexes = {
        @Index(name = "idx_employees_department", columnList = "department_id"),
        @Index(name = "idx_employees_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private EmployeeType type;

    // Added as a @Transient field to fulfill the Strategy Pattern requirements from
    // docs/01_core_design.md
    // This allows the Strategy implementations to pull metrics directly from the
    // Employee object,
    // protecting the SalaryCalculator interface from modification (Open/Closed
    // Principle).
    @Transient
    private Double hoursWorked;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @Column(name = "base_salary", precision = 15, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToMany(mappedBy = "employees")
    @Builder.Default
    private Set<Project> projects = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Employee employee = (Employee) o;
        return getId() != null && getId().equals(employee.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
