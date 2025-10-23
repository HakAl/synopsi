package com.study.synopsi.exception;

public class SummaryNotFoundException extends RuntimeException {
    public SummaryNotFoundException(Long id) {
        super("Summary not found with id: " + id);
    }
}