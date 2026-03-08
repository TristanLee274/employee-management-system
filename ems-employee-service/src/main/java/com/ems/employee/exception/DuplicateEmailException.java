package com.ems.employee.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when attempting to create an employee with an email that already
 * exists.
 *
 * <p>
 * Maps to HTTP 409 Conflict — the request could not be completed due to a state
 * conflict with the current state of the resource (duplicate email).
 * </p>
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateEmailException extends RuntimeException {

    private final String email;

    public DuplicateEmailException(String email) {
        super("An employee with email '%s' already exists".formatted(email));
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
