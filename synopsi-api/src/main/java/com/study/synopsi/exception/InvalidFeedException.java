package com.study.synopsi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidFeedException extends InvalidRequestException {
    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidFeedException(String message) {
        super(message);
    }

    public InvalidFeedException(String message, Throwable cause) {
        super(message, cause);
    }
}