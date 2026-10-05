package com.study.synopsi.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for API errors
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle authentication failures (401)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid username/email or password",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle disabled account (401)
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Account is disabled",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle locked account (401)
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(LockedException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Account is locked",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Any missing resource (404). Every *NotFoundException extends
     * ResourceNotFoundException, so one handler covers them all.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Request conflicts with existing state (409): a duplicate email, an
     * article URL already ingested, a summary job already queued, a retry of
     * a job that is not FAILED. ArticleAlreadyExistsException is a subclass.
     */
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ErrorResponse> handleResourceConflict(ResourceConflictException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Business rule violation the caller can fix (400): wrong current
     * password, invalid or expired reset token.
     */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle database integrity violations.
     * A unique-constraint violation (the check-then-act race where two requests
     * pass an existence check and the loser hits the index) is a 409 so workers
     * can treat it as a duplicate. Any other integrity failure (missing required
     * value, value too long, bad reference) is a 400: the request data is wrong
     * and must not be mistaken for a duplicate, or the worker would skip the
     * article forever. Classification is by SQLSTATE only. Driver details are
     * logged, not returned.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        Throwable root = ex.getMostSpecificCause();
        String detail = root != null && root.getMessage() != null ? root.getMessage() : String.valueOf(ex.getMessage());

        if (isUniqueConstraintViolation(ex)) {
            log.warn("Unique constraint violation: {}", detail);
            ErrorResponse error = new ErrorResponse(
                    HttpStatus.CONFLICT.value(),
                    "Request conflicts with an existing record",
                    LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        log.warn("Data integrity violation: {}", detail);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Request violates a data constraint (missing required value, value too long, or invalid reference)",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Structured classification only. The first SQLException in the cause chain
     * is authoritative: SQLSTATE 23505 (unique_violation in H2 and PostgreSQL)
     * means a duplicate, any other state means something else went wrong.
     * Error text is never consulted: drivers embed the rejected value in
     * messages (H2's 22001 "Value too long" does), so a title that happens to
     * start with "Unique" must not be classified as a duplicate.
     * No SQLException at all (for example Hibernate rejecting a null
     * non-nullable property before the statement runs) is not a duplicate.
     */
    private boolean isUniqueConstraintViolation(DataIntegrityViolationException ex) {
        Throwable t = ex;
        while (t != null) {
            if (t instanceof SQLException sqlEx) {
                return UNIQUE_VIOLATION_SQLSTATE.equals(sqlEx.getSQLState());
            }
            if (t.getCause() == t) {
                break;
            }
            t = t.getCause();
        }
        return false;
    }

    private static final String UNIQUE_VIOLATION_SQLSTATE = "23505";

    /**
     * Handle validation errors from @Valid (400)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, Object> errors = new HashMap<>();
        errors.put("timestamp", LocalDateTime.now());
        errors.put("status", HttpStatus.BAD_REQUEST.value());
        errors.put("error", "Validation Failed");

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        errors.put("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Unparseable or missing request body (400). Without this a malformed JSON
     * payload reached the generic handler as a 500 and was retried by the client.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.debug("Unreadable request body: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Malformed request body",
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handle invalid enum values, date formats, etc. (400)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        String message = String.format(
                "Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(),
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handle IllegalArgumentException (e.g., invalid date range) (400)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Anything not mapped above is a bug or an outage (500). Business
     * failures must never reach here: the dashboard's fetchWithRetry retries
     * every 5xx three times, so a 500 for "wrong password" meant three
     * password checks per click. Auth failures used to be recognised here by
     * sniffing the message text; AuthService now throws Spring Security's
     * BadCredentialsException, DisabledException and LockedException, which
     * have their own handlers. The stack trace is logged, since a 500 with no
     * log line is undiagnosable; the internal message is not returned.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Standard error response structure
     */
    public record ErrorResponse(
            int status,
            String message,
            LocalDateTime timestamp
    ) {}
}