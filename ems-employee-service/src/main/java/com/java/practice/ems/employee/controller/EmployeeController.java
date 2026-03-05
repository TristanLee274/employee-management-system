package com.java.practice.ems.employee.controller;

import com.java.practice.ems.common.PageResponse;
import com.java.practice.ems.employee.dto.CreateEmployeeRequest;
import com.java.practice.ems.employee.dto.EmployeeListResponse;
import com.java.practice.ems.employee.dto.EmployeeResponse;
import com.java.practice.ems.employee.dto.UpdateEmployeeRequest;
import com.java.practice.ems.employee.service.EmployeeService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ EmployeeController — REST API ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 * 
 * Defines REST endpoints.
 * It uses Java 21 Records for request and response DTOs because they guarantee
 * immutability preventing side effects, and cleanly deserialize from Spring
 * JSON inputs.
 * Spring Data generates implementation abstractions so we don't have to write
 * queries.
 */
@RestController
@RequestMapping("/api/v1/employees")
@SuppressWarnings("null")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<EmployeeListResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(PageResponse.from(employeeService.findAll(page, size, sortBy, sortDir)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable String id) {
        var response = employeeService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<EmployeeResponse> create(
            @Valid @RequestBody CreateEmployeeRequest request) {
        var response = employeeService.create(request);
        var location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@PathVariable String id) {
        employeeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
