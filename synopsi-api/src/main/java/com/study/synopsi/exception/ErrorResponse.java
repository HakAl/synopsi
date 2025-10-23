package com.study.synopsi.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * RFC 7807 Problem Details for HTTP APIs
 * Standard error response format
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * A URI reference that identifies the problem type
     */
    private String type;

    /**
     * A short, human-readable summary of the problem type
     */
    private String title;

    /**
     * The HTTP status code
     */
    private Integer status;

    /**
     * A human-readable explanation specific to this occurrence of the problem
     */
    private String detail;

    /**
     * A URI reference that identifies the specific occurrence of the problem
     */
    private String instance;

    /**
     * Timestamp when the error occurred
     */
    private Instant timestamp;

    /**
     * Additional details about validation errors (field-level errors)
     * Key: field name, Value: error message
     */
    private Map<String, String> errors;

    /**
     * Factory method for simple error responses
     */
    public static ErrorResponse of(String type, String title, Integer status, String detail, String instance) {
        return ErrorResponse.builder()
                .type(type)
                .title(title)
                .status(status)
                .detail(detail)
                .instance(instance)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Factory method for validation error responses with field errors
     */
    public static ErrorResponse withValidationErrors(String type, String title, Integer status,
                                                     String detail, String instance, Map<String, String> errors) {
        return ErrorResponse.builder()
                .type(type)
                .title(title)
                .status(status)
                .detail(detail)
                .instance(instance)
                .timestamp(Instant.now())
                .errors(errors)
                .build();
    }
}