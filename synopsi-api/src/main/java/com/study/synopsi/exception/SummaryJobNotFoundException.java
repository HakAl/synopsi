package com.study.synopsi.exception;

import java.io.Serial;

public class SummaryJobNotFoundException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public SummaryJobNotFoundException(Long id) {
        super("Summary job not found with id: " + id);
    }
}