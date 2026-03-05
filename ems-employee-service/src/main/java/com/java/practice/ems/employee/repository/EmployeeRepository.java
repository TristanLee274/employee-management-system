package com.java.practice.ems.employee.repository;

import com.java.practice.ems.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ Employee Repository — Data Access Layer ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>CHOICE OF SPRING DATA JPA</h2>
 * <p>
 * Spring Data JPA is chosen for the persistence layer because it significantly
 * reduces boilerplate code by dynamically generating implementations for
 * standard
 * CRUD and pagination operations. By using an interface-based approach, it
 * enforces
 * the Repository Pattern and provides out-of-the-box, optimized SQL generation.
 * </p>
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String>,
                JpaSpecificationExecutor<Employee> {

        Optional<Employee> findByEmail(String email);

        boolean existsByEmail(String email);

        @Query("SELECT e FROM Employee e WHERE e.deleted = false AND (LOWER(e.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        org.springframework.data.domain.Page<Employee> searchByKeyword(@Param("keyword") String keyword,
                        org.springframework.data.domain.Pageable pageable);

        @Query("SELECT e FROM Employee e WHERE e.deleted = false AND e.department.id = :deptId")
        List<Employee> findActiveByDepartmentId(@Param("deptId") String deptId);

        @Query(value = """
                            SELECT d.name AS departmentName, COUNT(e.id) AS employeeCount,
                                   AVG(e.base_salary) AS avgSalary
                            FROM employees e JOIN departments d ON e.department_id = d.id
                            WHERE e.deleted = false GROUP BY d.name
                        """, nativeQuery = true)
        List<DepartmentStatsProjection> getDepartmentStatistics();
}
