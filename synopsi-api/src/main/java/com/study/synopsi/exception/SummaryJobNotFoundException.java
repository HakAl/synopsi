package com.study.synopsi.exception;

public class SummaryJobNotFoundException extends RuntimeException {
    public SummaryJobNotFoundException(Long id) {
        super("Summary job not found with id: " + id);
    }
}