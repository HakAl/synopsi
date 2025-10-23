package com.study.synopsi.exception;

/**
 * Abstract base exception for all custom application exceptions.
 * Provides common structure for exception hierarchy.
 */
public abstract class BaseException extends RuntimeException {

    public BaseException(String message) {
        super(message);
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
    }
}