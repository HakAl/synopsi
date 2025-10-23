package com.study.synopsi.exception;

/**
 * Abstract base exception for resource not found errors (HTTP 404).
 * Extend this class for specific resource types (Article, Feed, Source, etc.).
 */
public abstract class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}