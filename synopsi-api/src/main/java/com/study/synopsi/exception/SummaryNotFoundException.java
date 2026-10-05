package com.study.synopsi.exception;

import java.io.Serial;

public class SummaryNotFoundException extends ResourceNotFoundException {
    @Serial
    private static final long serialVersionUID = 1L;

    public SummaryNotFoundException(Long id) {
        super("Summary not found with id: " + id);
    }
}