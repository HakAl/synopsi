package com.study.synopsi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FeedNotFoundException extends ResourceNotFoundException {

    public FeedNotFoundException(Long id) {
        super(String.format("Feed not found with id: %d", id));
    }

    public FeedNotFoundException(String message) {
        super(message);
    }
}