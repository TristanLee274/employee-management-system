package com.ems.employee.repository;

import com.ems.employee.model.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * <h2>CHOICE OF SPRING DATA JPA</h2>
 * <p>
 * Spring Data JPA simplifies data access by generating standard CRUD
 * implementations,
 * eliminating verbose native JDBC/SQL code while strictly adhering to the
 * Repository Pattern.
 * </p>
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
}
