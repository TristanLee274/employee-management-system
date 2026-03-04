package com.java.practice.ems.employee.repository;

import com.java.practice.ems.employee.entity.Employee;
import com.java.practice.ems.employee.entity.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
 * <h2>SPRING DATA JPA: THE REPOSITORY PATTERN</h2>
 *
 * <p>
 * This interface implements the <strong>Repository Pattern</strong>, one of the
 * most
 * powerful abstractions in Spring Data. Here's how it works and why it matters:
 * </p>
 *
 * <h3>What Spring Data JPA Does Behind the Scenes</h3>
 * <ol>
 * <li><strong>Proxy generation at startup:</strong> Spring scans for interfaces
 * extending
 * {@link JpaRepository}. For each one, it generates a <em>runtime proxy
 * class</em>
 * (using Java's {@code Proxy} or CGLIB) that implements all declared
 * methods.</li>
 * <li><strong>Query derivation:</strong> Method names like
 * {@code findByEmail(String email)}
 * are parsed into JPQL queries:
 * {@code SELECT e FROM Employee e WHERE e.email = ?1}.
 * No SQL writing needed for standard operations.</li>
 * <li><strong>Transaction management:</strong> Every repository method runs
 * inside a
 * transaction by default ({@code @Transactional(readOnly=true)} for reads,
 * {@code @Transactional} for writes). You get ACID guarantees for free.</li>
 * <li><strong>Connection pool integration:</strong> HikariCP provides the JDBC
 * connections.
 * With Virtual Threads enabled, blocked connections don't waste OS
 * threads.</li>
 * </ol>
 *
 * <h3>How This Decouples Business Logic from Data Access</h3>
 *
 * <p>
 * The Service layer depends on this <em>interface</em>, not on any concrete
 * implementation.
 * This follows the <strong>Dependency Inversion Principle (SOLID)</strong>:
 * </p>
 *
 * <pre>
 *   EmployeeService (high-level) → depends on → EmployeeRepository (abstraction)
 *                                                        ↑
 *                                   Spring-generated proxy (low-level detail)
 * </pre>
 *
 * <p>
 * Benefits of this decoupling:
 * </p>
 * <ul>
 * <li><strong>Testability:</strong> In unit tests, you mock this interface —
 * no database needed.
 * {@code Mockito.when(repo.findById(1L)).thenReturn(...)}</li>
 * <li><strong>Swappability:</strong> Switch from PostgreSQL to MySQL by
 * changing only the
 * JDBC URL and dialect — this interface doesn't change.</li>
 * <li><strong>Focus:</strong> The Service layer contains business rules
 * ("employee salary
 * cannot exceed department budget"). The Repository layer handles "how to
 * store/retrieve
 * data". Each changes for different reasons (Single Responsibility).</li>
 * </ul>
 *
 * <h3>Why We Extend JpaRepository (Not CrudRepository)</h3>
 *
 * <p>
 * {@link JpaRepository} extends {@code PagingAndSortingRepository} which
 * extends
 * {@code CrudRepository}. This gives us:
 * </p>
 * <ul>
 * <li>All CRUD methods: {@code save}, {@code findById}, {@code delete},
 * {@code count}</li>
 * <li>Pagination: {@code findAll(Pageable)} for paginated employee lists</li>
 * <li>Sorting: {@code findAll(Sort.by("lastName"))} for sorted results</li>
 * <li>Batch operations: {@code saveAll}, {@code deleteAllInBatch} for bulk
 * efficiency</li>
 * <li>JPA-specific: {@code flush}, {@code getById} (returns proxy
 * reference)</li>
 * </ul>
 *
 * @see JpaRepository for inherited methods
 * @see org.springframework.data.jpa.repository.support.SimpleJpaRepository for
 *      the
 *      default implementation Spring generates at runtime
 */
@Repository // Marks this as a Spring-managed persistence component and enables
            // exception translation (converts raw JDBC/JPA exceptions into
            // Spring's DataAccessException hierarchy for consistent error handling).
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // ═══════════════════════════════════════════════════════════════════════════
    // QUERY DERIVATION — Spring Data parses method names into SQL
    // ═══════════════════════════════════════════════════════════════════════════
    //
    // Spring Data's query derivation engine reads the method name and generates
    // the corresponding JPQL query. The naming convention is:
    // findBy{FieldName}{Condition}(parameter)
    //
    // This eliminates boilerplate SQL while remaining type-safe and
    // refactor-friendly.
    // If you rename "email" in the Entity, the compiler catches the broken method
    // name.
    //

    /**
     * Find an employee by their unique email address.
     * <p>
     * Generated JPQL: {@code SELECT e FROM Employee e WHERE e.email = :email}
     * <p>
     * Returns {@link Optional} instead of {@code null} to enforce explicit
     * null-handling
     * at the call site, preventing NullPointerException in the Service layer.
     *
     * @param email the employee's email address (case-sensitive)
     * @return an Optional containing the employee if found, or empty if not
     */
    Optional<Employee> findByEmail(String email);

    /**
     * Check if an employee with the given email already exists.
     * <p>
     * Generated SQL: {@code SELECT COUNT(*) > 0 FROM employees WHERE email = ?}
     * <p>
     * More efficient than {@code findByEmail().isPresent()} because it doesn't
     * load the full entity — just checks existence. Used for validation before
     * creating a new employee to provide a user-friendly error message instead
     * of a database constraint violation exception.
     *
     * @param email the email to check
     * @return true if an employee with this email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find all employees in a specific department.
     * <p>
     * Generated JPQL:
     * {@code SELECT e FROM Employee e WHERE e.department = :department}
     * <p>
     * The {@code idx_employees_department} index (defined in the Entity's @Table)
     * ensures this query performs an index scan, not a full table scan.
     *
     * @param department the department name
     * @return list of employees in the department (empty list if none found)
     */
    List<Employee> findByDepartment(String department);

    /**
     * Find all employees with a specific status, with pagination.
     * <p>
     * {@link Pageable} enables:
     * <ul>
     * <li>Offset pagination: {@code PageRequest.of(0, 20)} — first 20 results</li>
     * <li>Sorting: {@code PageRequest.of(0, 20, Sort.by("lastName"))} — sorted</li>
     * <li>Total count: {@code page.getTotalElements()} — for UI pagination
     * controls</li>
     * </ul>
     * <p>
     * PostgreSQL generates:
     * {@code SELECT * FROM employees WHERE status = ? ORDER BY ... LIMIT ? OFFSET ?}
     *
     * @param status   the employment status to filter by
     * @param pageable pagination and sorting parameters
     * @return a page of employees matching the status
     */
    Page<Employee> findByStatus(EmployeeStatus status, Pageable pageable);

    /**
     * Find employees whose first or last name contains the search term
     * (case-insensitive).
     * <p>
     * {@code Containing} maps to SQL {@code LIKE '%term%'}.
     * {@code IgnoreCase} adds {@code LOWER()} wrapping for case-insensitive
     * matching.
     * <p>
     * Generated JPQL:
     * 
     * <pre>
     *   SELECT e FROM Employee e
     *   WHERE LOWER(e.firstName) LIKE LOWER(CONCAT('%', :name, '%'))
     *      OR LOWER(e.lastName)  LIKE LOWER(CONCAT('%', :name, '%'))
     * </pre>
     *
     * @param firstName the search term for first name
     * @param lastName  the search term for last name (typically the same value)
     * @param pageable  pagination parameters
     * @return paginated search results
     */
    Page<Employee> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName, Pageable pageable);

    // ═══════════════════════════════════════════════════════════════════════════
    // CUSTOM JPQL QUERIES — For complex logic that method names can't express
    // ═══════════════════════════════════════════════════════════════════════════
    //
    // When query derivation produces unreadable method names (like the search
    // method above), or when you need JOINs, subqueries, or aggregations,
    // use @Query with JPQL (Java Persistence Query Language).
    //
    // JPQL is database-agnostic — it operates on entities and fields, not tables
    // and columns. Spring Data translates it to PostgreSQL-specific SQL at runtime.
    //

    /**
     * Search employees by keyword across multiple fields.
     * <p>
     * This is a cleaner alternative to the long derived method name above.
     * The {@code @Query} annotation provides full control over the JPQL query.
     *
     * @param keyword  the search keyword (matched against name, email, department,
     *                 job title)
     * @param pageable pagination and sorting parameters
     * @return paginated search results
     */
    @Query("""
                SELECT e FROM Employee e
                WHERE LOWER(e.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(e.lastName)  LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(e.email)     LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(e.department) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(e.jobTitle)  LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Employee> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Count employees by department — useful for dashboard statistics.
     * <p>
     * Generated SQL: {@code SELECT COUNT(*) FROM employees WHERE department = ?}
     *
     * @param department the department name
     * @return number of employees in the department
     */
    long countByDepartment(String department);

    /**
     * Count employees by status — useful for HR dashboards.
     *
     * @param status the employment status
     * @return number of employees with the given status
     */
    long countByStatus(EmployeeStatus status);

    /**
     * Find all employees in a department with a specific status.
     * <p>
     * Demonstrates compound query derivation: Spring chains conditions with AND.
     * Generated JPQL:
     * {@code SELECT e FROM Employee e WHERE e.department = :dept AND e.status = :status}
     *
     * @param department the department name
     * @param status     the employment status
     * @return list of matching employees
     */
    List<Employee> findByDepartmentAndStatus(String department, EmployeeStatus status);
}
