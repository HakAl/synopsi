package com.study.synopsi.exception;

public class TopicNotFoundException extends RuntimeException {
    
    public TopicNotFoundException(Long id) {
        super("Topic not found with id: " + id);
    }
    
    public TopicNotFoundException(String slug) {
        super("Topic not found with slug: " + slug);
    }
    
    public TopicNotFoundException(String field, String value) {
        super(String.format("Topic not found with %s: %s", field, value));
    }
}