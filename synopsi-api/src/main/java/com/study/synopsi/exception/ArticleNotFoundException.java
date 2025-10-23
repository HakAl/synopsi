package com.study.synopsi.exception;

/**
 * Exception thrown when an Article is not found by ID.
 * Maps to HTTP 404 NOT_FOUND.
 */
public class ArticleNotFoundException extends ResourceNotFoundException {

    public ArticleNotFoundException(Long id) {
        super("Article not found with id: " + id);
    }

    public ArticleNotFoundException(String message) {
        super(message);
    }
}