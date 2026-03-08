package com.ems.employee.controller;


import com.ems.employee.dto.response.PageResponse;
import com.ems.employee.dto.request.CreateEmployeeRequest;
import com.ems.employee.dto.response.EmployeeListResponse;
import com.ems.employee.dto.response.EmployeeResponse;
import com.ems.employee.dto.request.UpdateEmployeeRequest;
import com.ems.employee.service.EmployeeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ EmployeeController — RESTful API (Thin Controller Pattern) ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>SINGLE RESPONSIBILITY PRINCIPLE (SRP)</h2>
 *
 * <p>
 * This controller has ONE responsibility: <strong>HTTP translation</strong>.
 * It translates HTTP requests into service calls and service results into HTTP
 * responses. It contains ZERO business logic — all rules live in
 * {@link EmployeeService}.
 * </p>
 *
 * <p>
 * <strong>Why "Thin Controller"?</strong> Fat controllers are the #1 cause of
 * unmaintainable codebases. When business logic creeps into controllers:
 * </p>
 * <ul>
 * <li>You can't unit test the logic without spinning up Spring MVC</li>
 * <li>Logic gets duplicated if multiple endpoints share behavior</li>
 * <li>The controller violates SRP — it does HTTP + business rules</li>
 * </ul>
 *
 * <h2>REST API DESIGN DECISIONS</h2>
 *
 * <ul>
 * <li><strong>Java 21 Records for DTOs:</strong> Request/Response DTOs are
 * Records — immutable, concise, thread-safe</li>
 * <li><strong>Bean Validation:</strong> {@code @Valid} triggers constraint
 * checks before the service is called — fail fast</li>
 * <li><strong>Location header on POST:</strong> Returns URI of created
 * resource per HTTP 201 specification (RFC 7231 §6.3.2)</li>
 * <li><strong>Soft delete via PATCH/DELETE:</strong> DELETE doesn't remove
 * data — it deactivates the employee (audit trail preserved)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Employee Management", description = "CRUD operations for employee records")
@SuppressWarnings("null")
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * Constructor injection — preferred over field injection for immutability
     * and explicit dependency declaration (see docs/01_core_design.md §DIP).
     */
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * Lists employees with pagination and sorting.
     * Default: page 0, 20 items/page, sorted by fullName ASC.
     */
    @GetMapping
    @Operation(summary = "List all employees with pagination and sorting")
    public ResponseEntity<PageResponse<EmployeeListResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(PageResponse.from(employeeService.findAll(page, size, sortBy, sortDir)));
    }

    /**
     * Retrieves a single employee by ID. Returns 404 if not found.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get employee by ID")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable String id) {
        var response = employeeService.findById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new employee. Returns 201 Created with Location header
     * pointing to the new resource (REST standard).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new employee")
    public ResponseEntity<EmployeeResponse> create(
            @Valid @RequestBody CreateEmployeeRequest request) {
        var response = employeeService.create(request);
        // RFC 7231 §6.3.2: 201 responses SHOULD include a Location header
        var location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Updates an existing employee (partial update — PATCH semantics).
     * Only non-null fields in the request will be updated.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing employee")
    public ResponseEntity<EmployeeResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    /**
     * Soft-deletes (deactivates) an employee. Returns 204 No Content.
     *
     * <p>
     * Design decision: We use soft delete (status → INACTIVE) instead of
     * hard delete to preserve audit trail and comply with data retention
     * policies. The employee record remains in the database but is excluded
     * from active queries.
     * </p>
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate (soft-delete) an employee")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        employeeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
