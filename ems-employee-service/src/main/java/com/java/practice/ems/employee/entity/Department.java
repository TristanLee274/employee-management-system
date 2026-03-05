package com.java.practice.ems.employee.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "departments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Department parent; // Self-ref tree

    @OneToMany(mappedBy = "department")
    private List<Employee> employees;
}
