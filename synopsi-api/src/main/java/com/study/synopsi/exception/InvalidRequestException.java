package com.study.synopsi.exception;

/**
 * Exception thrown for invalid requests or business rule violations.
 * Maps to HTTP 400 BAD_REQUEST.
 */
public class InvalidRequestException extends BaseException {

    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}