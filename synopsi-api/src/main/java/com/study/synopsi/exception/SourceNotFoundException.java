package com.study.synopsi.exception;

import java.io.Serial;

public class SourceNotFoundException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;
    
    public SourceNotFoundException(Long id) {
        super("Source not found with id: " + id);
    }
    
    public SourceNotFoundException(String name) {
        super("Source not found with name: " + name);
    }
    
    public SourceNotFoundException(String field, String value) {
        super(String.format("Source not found with %s: %s", field, value));
    }
}