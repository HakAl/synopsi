package com.study.synopsi.exception;

public class SourceNotFoundException extends RuntimeException {
    
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