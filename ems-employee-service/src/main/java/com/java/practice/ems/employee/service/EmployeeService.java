package com.java.practice.ems.employee.service;

import com.java.practice.ems.employee.dto.*;
import com.java.practice.ems.employee.entity.Employee;
import com.java.practice.ems.employee.entity.EmployeeStatus;
import com.java.practice.ems.employee.exception.DuplicateEmailException;
import com.java.practice.ems.employee.exception.EmployeeNotFoundException;
import com.java.practice.ems.employee.repository.EmployeeRepository;
import com.java.practice.ems.employee.salary.SalaryCalculator;
import com.java.practice.ems.employee.salary.SalaryCalculatorFactory;
import com.java.practice.ems.employee.event.EventPublisher;
import com.java.practice.ems.kafka.EmployeeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ EmployeeService — Business Logic Layer ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>@SERVICE ANNOTATION — ROLE IN SPRING</h2>
 *
 * <p>
 * {@code @Service} is a specialization of {@code @Component} with two key
 * roles:
 * </p>
 * <ol>
 * <li><strong>Component scanning:</strong> Spring registers this class as a
 * bean in
 * the application context. Any class annotated with {@code @Autowired} or
 * receiving it via constructor injection gets the same singleton instance.</li>
 * <li><strong>Semantic clarity:</strong> Signals that this class lives in the
 * <em>service layer</em> — containing business logic, orchestrating
 * repositories,
 * and enforcing business rules. AOP proxies (transactions, security, caching)
 * target this layer specifically.</li>
 * </ol>
 *
 * <h3>Transaction Management via @Service + @Transactional</h3>
 *
 * <p>
 * When {@code @Transactional} is combined with {@code @Service}, Spring wraps
 * every
 * annotated method in an AOP proxy that:
 * </p>
 * <ol>
 * <li>Opens a JDBC connection and starts a transaction (BEGIN)</li>
 * <li>Executes the method body (your business logic + repository calls)</li>
 * <li>On success: commits the transaction (COMMIT)</li>
 * <li>On RuntimeException: rolls back ALL changes (ROLLBACK) — atomicity
 * guaranteed</li>
 * <li>Returns the connection to the HikariCP pool</li>
 * </ol>
 *
 * <p>
 * This means even complex operations like "create employee + publish Kafka
 * event
 * + update audit log" are atomic — either all succeed or all are rolled back.
 * </p>
 *
 * <h2>DEPENDENCY INVERSION PRINCIPLE (DIP) IN THIS CLASS</h2>
 *
 * <p>
 * All dependencies are injected as <strong>interfaces</strong>, never concrete
 * classes:
 * </p>
 * <ul>
 * <li>{@code EmployeeRepository} — interface (Spring Data generates the
 * implementation)</li>
 * <li>{@code SalaryCalculatorFactory} — depends on {@code SalaryCalculator}
 * interface internally</li>
 * </ul>
 *
 * <p>
 * The service is completely unaware of:
 * </p>
 * <ul>
 * <li>Which database is used (PostgreSQL, H2, MySQL)</li>
 * <li>Which specific calculator algorithm runs for a given employee type</li>
 * <li>How SQL is generated or executed</li>
 * </ul>
 *
 * <p>
 * This enables full unit testing without any database or Spring context.
 * </p>
 *
 * <h2>SINGLE RESPONSIBILITY PRINCIPLE</h2>
 *
 * <p>
 * This service has ONE responsibility: <strong>orchestrate employee business
 * operations</strong>.
 * It delegates:
 * </p>
 * <ul>
 * <li>Data persistence → {@code EmployeeRepository}</li>
 * <li>Salary computation → {@code SalaryCalculatorFactory} + Strategy
 * implementations</li>
 * <li>HTTP concerns → Controller layer (not here)</li>
 * <li>Event publishing → generic {@code EventPublisher} interface (DIP
 * enforcement)</li>
 * </ul>
 * 
 * <h2>OBSERVER PATTERN — KAFKA AS A DISTRIBUTED IMPLEMENTATION</h2>
 * 
 * <p>
 * This class delegates the cross-service notification of events to the Observer
 * {@code EventPublisher}. This separates state mutations (like persisting the
 * employee) from cross-cutting integration side-effects (notifying other
 * domains via Kafka).
 * </p>
 */
@Service
@Slf4j // Lombok: generates: private static final Logger log =
       // LoggerFactory.getLogger(EmployeeService.class);
@Transactional(readOnly = true)
// CLASS-LEVEL @Transactional(readOnly = true):
// All methods in this class default to read-only transactions.
// Benefits:
// • Spring tells HikariCP this connection won't write → pool can optimize
// connection reuse
// • Hibernate disables dirty checking (doesn't scan entities for changes at
// flush time)
// • PostgreSQL can route read-only transactions to replica nodes in a
// read-replica setup
// Individual write methods override with @Transactional(readOnly = false)
// explicitly.
@SuppressWarnings("null")
public class EmployeeService {

    // ═══════════════════════════════════════════════════════════════════════════
    // DEPENDENCIES — Injected via constructor (Dependency Inversion Principle)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    // WHY CONSTRUCTOR INJECTION (not @Autowired field injection):
    //
    // 1. IMMUTABILITY: Fields are final — once set in the constructor, they cannot
    // be reassigned. This prevents accidental null-assignment bugs.
    //
    // 2. TESTABILITY: In unit tests, create the service directly:
    // new EmployeeService(mockRepository, mockFactory)
    // No Spring context required. Field injection (@Autowired) requires Spring.
    //
    // 3. EXPLICIT CONTRACT: The constructor signature documents what this class
    // NEEDS.
    // A developer reading this knows exactly the dependencies at a glance.
    //
    // 4. CIRCULAR DEPENDENCY DETECTION: Spring detects circular dependencies at
    // startup with constructor injection (fails fast). Field injection hides them.
    //
    // WHY INTERFACES (not concrete classes):
    // → Follows DIP: this high-level service depends on abstractions, not details.
    // The JPA implementation of EmployeeRepository can be swapped transparently.
    //

    private final EmployeeRepository employeeRepository;
    private final SalaryCalculatorFactory salaryCalculatorFactory;
    private final EventPublisher eventPublisher;

    public EmployeeService(EmployeeRepository employeeRepository,
            SalaryCalculatorFactory salaryCalculatorFactory,
            EventPublisher eventPublisher) {
        this.employeeRepository = employeeRepository;
        this.salaryCalculatorFactory = salaryCalculatorFactory;
        this.eventPublisher = eventPublisher;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // READ OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Retrieves the full details of a single employee by ID.
     *
     * <p>
     * Runs in a read-only transaction (inherited from class-level annotation).
     * Throws {@link EmployeeNotFoundException} (HTTP 404) if not found.
     * </p>
     *
     * @param id the employee's primary key
     * @return the employee response DTO
     */
    @org.springframework.security.access.prepost.PreAuthorize("#id == authentication.principal.claims['sub'] or hasRole('HR') or hasRole('ADMIN')")
    public EmployeeResponse findById(@org.springframework.lang.NonNull String id) {
        log.debug("Fetching employee with id={}", id);

        return employeeRepository.findById(id)
                .map(EmployeeResponse::from) // Entity → DTO via factory method
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    /**
     * Returns a paginated, sorted list of all employees.
     *
     * <p>
     * Uses the lightweight {@link EmployeeListResponse} DTO instead of the full
     * {@link EmployeeResponse} — excludes salary. Follows ISP: list consumers
     * get exactly the data they need, nothing more.
     * </p>
     *
     * @param page      zero-based page number
     * @param size      page size (number of items per page)
     * @param sortBy    field name to sort by (e.g., "lastName", "createdAt")
     * @param direction sort direction ("ASC" or "DESC")
     * @return paginated list of employee summaries
     */
    public Page<EmployeeListResponse> findAll(int page, int size, String sortBy, String direction) {
        // ── Java 21 Pattern Matching for switch — string to Sort.Direction ───────
        //
        // BEFORE Java 21 (pre-SE-21):
        // Sort.Direction dir;
        // if ("DESC".equalsIgnoreCase(direction)) dir = Sort.Direction.DESC;
        // else dir = Sort.Direction.ASC;
        //
        // AFTER Java 21 — switch expression with null-safe case, concise, single
        // statement:
        //
        Sort.Direction sortDirection = switch (direction == null ? "ASC" : direction.toUpperCase()) {
            case "DESC" -> Sort.Direction.DESC;
            default -> Sort.Direction.ASC; // default to ASC for any unrecognized value
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        log.debug("Listing employees: page={}, size={}, sort={}({})", page, size, sortBy, sortDirection);

        return employeeRepository.findAll(pageable)
                .map(EmployeeListResponse::from); // Stream of entities → Stream of DTOs
    }

    /**
     * Searches employees by keyword across name, email, department, and job title
     * fields.
     *
     * @param keyword search term (min 1 character recommended)
     * @param page    page number
     * @param size    page size
     * @return paginated search results
     */
    public Page<EmployeeListResponse> search(String keyword, int page, int size) {
        log.debug("Searching employees with keyword='{}'", keyword);

        Pageable pageable = PageRequest.of(page, size, Sort.by("lastName").ascending());
        return employeeRepository.searchByKeyword(keyword, pageable)
                .map(EmployeeListResponse::from);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WRITE OPERATIONS — @Transactional(readOnly = false) overrides class default
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Creates a new employee record.
     *
     * <p>
     * Business rules enforced here (not in the controller or entity):
     * </p>
     * <ol>
     * <li>Email must be unique — checked before insert to provide a clear error
     * message</li>
     * <li>Hire date defaults to today if not provided</li>
     * <li>Default status is ACTIVE for new hires</li>
     * </ol>
     *
     * <p>
     * {@code @Transactional} (write): If any step fails, the entire operation rolls
     * back.
     * No partial employee records are left in the database.
     * </p>
     *
     * @param request validated inbound DTO (Bean Validation has already run in the
     *                controller)
     * @return the created employee as a response DTO
     * @throws DuplicateEmailException (HTTP 409) if email already exists
     */
    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public EmployeeResponse create(CreateEmployeeRequest request) {
        log.info("Creating employee with email={}", request.email());

        // ── Business Rule: Email uniqueness ───────────────────────────────────
        // We check BEFORE the INSERT to give a user-friendly error message.
        // Without this, the DB unique constraint would fire, producing a cryptic
        // DataIntegrityViolationException that we'd have to parse.
        if (employeeRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        com.java.practice.ems.employee.entity.Department deptRef = new com.java.practice.ems.employee.entity.Department();
        deptRef.setId(request.departmentId());

        Employee employee = Employee.builder()
                .fullName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .department(deptRef)
                .type(request.type())
                .baseSalary(request.baseSalary())
                // Hire date defaults to today if not provided in the request
                .joinDate(request.joinDate() != null ? request.joinDate() : LocalDate.now())
                .status(EmployeeStatus.ACTIVE) // New employees always start as ACTIVE
                .build();

        Employee saved = employeeRepository.save(employee);

        log.info("Employee created successfully: id={}, email={}", saved.getId(), saved.getEmail());

        // ── OBSERVER PATTERN: Publish event to Kafka ─────────────────────────────
        // After the employee is persisted in PostgreSQL (source of truth), we publish
        // an event to Kafka. This decouples the notification/audit/analytics systems
        // from the employee creation flow:
        //
        // • The employee is saved FIRST (strong consistency in PostgreSQL)
        // • The event is published AFTER (eventual consistency for consumers)
        // • If Kafka publish fails, the employee is still saved — the event producer
        // handles retries asynchronously
        //
        // This follows the TRANSACTIONAL OUTBOX pattern at a basic level:
        // the database write is committed before // 2. Publish async event (Observer
        // Pattern decoupled via EventPublisher interface)
        eventPublisher.publishEvent(EmployeeEvent.created(
                UUID.randomUUID().toString(),
                saved.getId(),
                saved.getFullName(),
                saved.getEmail(),
                saved.getDepartment() != null ? saved.getDepartment().getName() : null,
                saved.getType() != null ? saved.getType().name() : null,
                saved.getBaseSalary()));

        return EmployeeResponse.from(saved);
    }

    /**
     * Partially updates an existing employee record.
     *
     * <p>
     * Only fields present (non-null) in the request are updated. Null fields
     * preserve the current value. This implements the HTTP PATCH semantics.
     * </p>
     *
     * <p>
     * <strong>Java 21 Pattern Matching for switch — Status parsing:</strong>
     * The status string from the API is parsed into {@link EmployeeStatus} using
     * a switch expression with guarded patterns. See implementation below.
     * </p>
     *
     * @param id      the employee to update
     * @param request partial update data (null fields = keep current value)
     * @return the updated employee as a response DTO
     * @throws EmployeeNotFoundException (HTTP 404) if employee not found
     * @throws DuplicateEmailException   (HTTP 409) if new email conflicts with
     *                                   another employee
     */
    @Transactional
    public EmployeeResponse update(@org.springframework.lang.NonNull String id, UpdateEmployeeRequest request) {
        log.info("Updating employee id={}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        // ── Apply only non-null fields (partial update / PATCH semantics) ────
        if (request.fullName() != null)
            employee.setFullName(request.fullName());
        if (request.phone() != null)
            employee.setPhone(request.phone());
        if (request.departmentId() != null) {
            com.java.practice.ems.employee.entity.Department deptRef = new com.java.practice.ems.employee.entity.Department();
            deptRef.setId(request.departmentId());
            employee.setDepartment(deptRef);
        }
        if (request.type() != null)
            employee.setType(request.type());
        if (request.baseSalary() != null)
            employee.setBaseSalary(request.baseSalary());
        if (request.joinDate() != null)
            employee.setJoinDate(request.joinDate());

        // ── Email update with uniqueness re-validation ───────────────────────
        if (request.email() != null && !request.email().equals(employee.getEmail())) {
            if (employeeRepository.existsByEmail(request.email())) {
                throw new DuplicateEmailException(request.email());
            }
            employee.setEmail(request.email());
        }

        // ── Status parsing using Java 21 Pattern Matching for switch ─────────
        //
        // OLD approach (Java 8):
        // try { status = EmployeeStatus.valueOf(str); }
        // catch (IllegalArgumentException e) { /* handle */ }
        //
        // NEW approach (Java 21) — switch expression with null handling and
        // a when-guard clause (guarded patterns) for validation:
        //
        if (request.status() != null) {
            EmployeeStatus newStatus = switch (request.status()) {
                // ── Guarded pattern: only match if the status is a known valid value ──
                // Java 21 'when' clause adds an additional condition to the case pattern.
                // This is like "case X, but only when Y is true".
                case String s when isValidStatus(s) ->
                    // Enum.valueOf is safe here because isValidStatus() already validated it
                    EmployeeStatus.valueOf(s);

                // ── Null guard ───────────────────────────────────────────────────────
                case null ->
                    employee.getStatus(); // Keep current status if null sent

                // ── Invalid status string → clear error message ──────────────────────
                default -> throw new IllegalArgumentException(
                        "Invalid employee status: '%s'. Valid values: ACTIVE, INACTIVE, ON_LEAVE, PROBATION, TERMINATED"
                                .formatted(request.status()));
            };
            employee.setStatus(newStatus);
        }

        // ── No explicit save() call needed! ──────────────────────────────────
        // Because we fetched the entity within an active @Transactional context,
        // Hibernate tracks it as a "managed entity". On transaction commit, Hibernate
        // automatically detects changes (dirty checking) and issues an UPDATE
        // statement.
        // This is the "Unit of Work" pattern — Hibernate batches changes and flushes
        // once.

        log.info("Employee updated successfully: id={}", employee.getId());

        return EmployeeResponse.from(employee);
    }

    /**
     * Deactivates an employee (soft delete — marks as INACTIVE instead of deleting
     * the record).
     *
     * <p>
     * <strong>Why soft delete instead of hard delete?</strong>
     * </p>
     * <ul>
     * <li>HR compliance: Employee records may need to be retained for years</li>
     * <li>Payroll history: Past salary references remain intact</li>
     * <li>Audit trails: Activity logs referencing this employee ID stay valid</li>
     * <li>Reversibility: An accidentally deactivated employee can be
     * reactivated</li>
     * </ul>
     *
     * @param id the employee to deactivate
     * @throws EmployeeNotFoundException (HTTP 404) if not found
     */
    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public void deactivate(@org.springframework.lang.NonNull String id) {
        log.info("Deactivating employee id={}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        // ── Java 21 Pattern Matching for switch — guard on current status ────────
        //
        // The 'when' clause allows guarded patterns: "match this case ONLY when
        // condition is true"
        // This replaces nested if-else inside switch cases.
        //
        switch (employee.getStatus()) {
            // Guard: only proceed if status is ACTIVE or PROBATION (deactivatable states)
            case ACTIVE, PROBATION -> {
                employee.setStatus(EmployeeStatus.INACTIVE);
                log.info("Employee id={} deactivated (was: {})", id, employee.getStatus());
            }
            // Already inactive — idempotent operation, just warn
            case INACTIVE -> log.warn("Employee id={} is already INACTIVE, no change made", id);
            // Terminated employees cannot be simply deactivated — different process
            // required
            case TERMINATED -> throw new IllegalStateException(
                    "Cannot deactivate a terminated employee (id=%s). Use the reactivation flow first.".formatted(id));
            // On leave — allowed, but log the unusual state change
            case ON_LEAVE -> {
                employee.setStatus(EmployeeStatus.INACTIVE);
                log.warn("Deactivating employee id={} who was ON_LEAVE", id);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SALARY CALCULATION — Strategy Pattern integration
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calculates the net monthly salary for an employee using the Strategy Pattern.
     *
     * <h3>STRATEGY PATTERN RUNTIME DISPATCH FLOW</h3>
     * 
     * <pre>
     *   Controller calls calculateSalary(id, type, hours)
     *           │
     *           ▼
     *   Service asks Factory: getCalculator("HOURLY")
     *           │
     *           ▼
     *   Factory returns: HourlySalaryCalculator (the concrete strategy)
     *           │
     *           ▼
     *   Service calls: calculator.calculate(baseSalary, hoursWorked)
     *           │
     *           ▼
     *   HourlySalaryCalculator.calculate() runs its specific algorithm
     *           │
     *           ▼
     *   Returns: net BigDecimal salary amount
     * </pre>
     *
     * <p>
     * The service NEVER knows about the calculation algorithm. It only delegates
     * to the strategy. This fulfills the Single Responsibility Principle: the
     * service
     * orchestrates, the strategy computes.
     * </p>
     *
     * @param employeeId     the target employee's ID
     * @param employmentType the salary calculation type ("MONTHLY", "HOURLY", etc.)
     * @param hoursWorked    actual hours worked (or achievement % for commission)
     * @return salary calculation result DTO
     */
    public SalaryCalculationResult calculateSalary(@org.springframework.lang.NonNull String employeeId,
            String employmentType, double hoursWorked) {
        log.debug("Calculating salary for employeeId={}, type={}, hours={}", employeeId, employmentType, hoursWorked);

        // Step 1: Retrieve the employee (read operation, uses read-only transaction)
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));

        // Ensure the employee has a salary base rate configured
        BigDecimal baseSalary = employee.getBaseSalary();
        if (baseSalary == null || baseSalary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "Employee id=%s has no base salary configured".formatted(employeeId));
        }

        // Step 2: Resolve the strategy via the factory
        // The factory returns the correct SalaryCalculator implementation based on the
        // type string.
        // The service doesn't know (or care) which concrete class this is.
        employee.setHoursWorked(hoursWorked);
        SalaryCalculator calculator = salaryCalculatorFactory.getCalculator(employmentType);

        // Step 3: Delegate calculation to the strategy
        BigDecimal netMonthly = calculator.calculate(employee);
        BigDecimal netAnnual = calculator.annualSalary(netMonthly);

        log.info("Salary calculated for employeeId={}: monthly={}, annual={}", employeeId, netMonthly, netAnnual);

        // Step 4: Return a structured result Record
        return new SalaryCalculationResult(
                employeeId,
                employee.getFullName(),
                employmentType,
                baseSalary,
                netMonthly,
                netAnnual,
                hoursWorked);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Validates whether the given string is a valid {@link EmployeeStatus} name.
     *
     * <p>
     * Used as a guard condition in the Java 21 switch expression inside
     * {@link #update(Long, UpdateEmployeeRequest)}.
     * </p>
     *
     * @param status the status string to validate
     * @return true if it matches an EmployeeStatus enum constant
     */
    private boolean isValidStatus(String status) {
        try {
            EmployeeStatus.valueOf(status);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
