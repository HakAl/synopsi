package com.study.synopsi.exception;

import java.io.Serial;

/**
 * Exception thrown when a request conflicts with the current state of a
 * resource: a duplicate of something that must be unique, or an operation
 * the resource's current state does not allow.
 * Maps to HTTP 409 CONFLICT.
 */
public class ResourceConflictException extends BaseException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ResourceConflictException(String message) {
        super(message);
    }

    public ResourceConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
