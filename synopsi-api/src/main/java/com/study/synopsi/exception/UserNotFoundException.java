package com.study.synopsi.exception;

import java.io.Serial;

/**
 * Exception thrown when a User is not found.
 * Maps to HTTP 404 NOT_FOUND.
 */
public class UserNotFoundException extends ResourceNotFoundException {
    @Serial
    private static final long serialVersionUID = 1L;

    public UserNotFoundException(Long id) {
        super("User not found: " + id);
    }

    public UserNotFoundException(String field, String value) {
        super(String.format("User not found with %s: %s", field, value));
    }
}
