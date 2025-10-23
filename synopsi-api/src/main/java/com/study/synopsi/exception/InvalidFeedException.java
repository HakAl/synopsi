package com.study.synopsi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidFeedException extends InvalidRequestException {

    public InvalidFeedException(String message) {
        super(message);
    }

    public InvalidFeedException(String message, Throwable cause) {
        super(message, cause);
    }
}