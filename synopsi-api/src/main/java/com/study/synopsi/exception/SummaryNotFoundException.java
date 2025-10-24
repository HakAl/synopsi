package com.study.synopsi.exception;

import java.io.Serial;

public class SummaryNotFoundException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public SummaryNotFoundException(Long id) {
        super("Summary not found with id: " + id);
    }
}