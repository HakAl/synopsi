package com.study.synopsi.exception;

import java.io.Serial;

/**
 * Exception thrown for invalid requests or business rule violations.
 * Maps to HTTP 400 BAD_REQUEST.
 */
public class InvalidRequestException extends BaseException {
    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}