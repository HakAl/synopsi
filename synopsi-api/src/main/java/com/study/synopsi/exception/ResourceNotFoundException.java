package com.study.synopsi.exception;

import java.io.Serial;

/**
 * Abstract base exception for resource not found errors (HTTP 404).
 * Extend this class for specific resource types (Article, Feed, Source, etc.).
 */
public abstract class ResourceNotFoundException extends BaseException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}