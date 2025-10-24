package com.study.synopsi.exception;

import java.io.Serial;

/**
 * Exception thrown when an Article is not found by ID.
 * Maps to HTTP 404 NOT_FOUND.
 */
public class ArticleNotFoundException extends ResourceNotFoundException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ArticleNotFoundException(Long id) {
        super("Article not found with id: " + id);
    }

    public ArticleNotFoundException(String message) {
        super(message);
    }
}