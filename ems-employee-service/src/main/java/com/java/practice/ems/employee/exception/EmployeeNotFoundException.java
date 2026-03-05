package com.java.practice.ems.employee.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an employee with the requested ID does not exist.
 *
 * <p>
 * {@code @ResponseStatus} maps this exception to HTTP 404 Not Found.
 * Spring's {@code @ExceptionHandler} or {@code @ControllerAdvice} can also
 * intercept this for a structured error response body.
 * </p>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class EmployeeNotFoundException extends RuntimeException {

    private final String employeeId;

    public EmployeeNotFoundException(String employeeId) {
        super("Employee not found with id: " + employeeId);
        this.employeeId = employeeId;
    }

    public String getEmployeeId() {
        return employeeId;
    }
}
