package com.study.synopsi.exception;

import java.io.Serial;

/**
 * Exception thrown when an article with the same original URL already exists.
 * Maps to HTTP 409 CONFLICT so ingestion workers can treat it as a duplicate
 * rather than a failure.
 */
public class ArticleAlreadyExistsException extends ResourceConflictException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ArticleAlreadyExistsException(String originalUrl) {
        super("Article already exists with originalUrl: " + originalUrl);
    }
}
