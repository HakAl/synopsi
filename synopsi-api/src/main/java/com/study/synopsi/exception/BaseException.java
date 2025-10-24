package com.study.synopsi.exception;

import java.io.Serial;

/**
 * Abstract base exception for all custom application exceptions.
 * Provides common structure for exception hierarchy.
 */
public abstract class BaseException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public BaseException(String message) {
        super(message);
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
    }
}