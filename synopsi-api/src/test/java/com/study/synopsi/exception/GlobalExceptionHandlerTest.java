package com.study.synopsi.exception;

import com.study.synopsi.service.AccessControlService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Handler-level contract: which exception type produces which status, and
 * that a 500 is logged with its stack trace rather than swallowed.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private ListAppender<ILoggingEvent> logEvents;
    private Logger handlerLogger;

    @BeforeEach
    void captureLog() {
        handlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        logEvents = new ListAppender<>();
        logEvents.start();
        handlerLogger.addAppender(logEvents);
    }

    @AfterEach
    void releaseLog() {
        handlerLogger.detachAppender(logEvents);
    }

    @Test
    @DisplayName("Every not-found exception is a ResourceNotFoundException")
    void notFoundHierarchy() {
        assertThat(new UserNotFoundException(1L)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(new ArticleNotFoundException(1L)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(new FeedNotFoundException(1L)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(new SourceNotFoundException(1L)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(new TopicNotFoundException(1L)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(new SummaryNotFoundException(1L)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(new SummaryJobNotFoundException(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Conflict and bad-request subclasses sit under their handled parents")
    void conflictAndBadRequestHierarchy() {
        assertThat(new ArticleAlreadyExistsException("u")).isInstanceOf(ResourceConflictException.class);
        assertThat(new InvalidFeedException("f")).isInstanceOf(InvalidRequestException.class);
        assertThat(new InvalidTopicHierarchyException("t")).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("Not found is 404 with the exception message")
    void resourceNotFoundIs404() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleResourceNotFound(new UserNotFoundException(42L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("User not found: 42");
    }

    @Test
    @DisplayName("Conflict is 409 with the exception message")
    void resourceConflictIs409() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleResourceConflict(new ResourceConflictException("Email already exists: a@b.c"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("Email already exists: a@b.c");
    }

    @Test
    @DisplayName("Invalid request is 400 with the exception message")
    void invalidRequestIs400() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleInvalidRequest(new InvalidRequestException("Current password is incorrect"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Current password is incorrect");
    }

    @Test
    @DisplayName("Spring Security account exceptions are 401")
    void authenticationFailuresAre401() {
        assertThat(handler.handleBadCredentials(new BadCredentialsException("x")).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(handler.handleDisabled(new DisabledException("x")).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(handler.handleLocked(new LockedException("x")).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Access denied is 403 with a fixed message that does not echo the exception")
    void accessDeniedIs403() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleAccessDenied(new AccessDeniedException("user 7 tried user 9"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().message())
                .isEqualTo(AccessControlService.FORBIDDEN_MESSAGE)
                .doesNotContain("user 7");
    }

    @Test
    @DisplayName("Missing credentials is 401")
    void missingCredentialsIs401() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleMissingCredentials(new AuthenticationCredentialsNotFoundException("none"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().message()).isEqualTo("Authentication required. Please login.");
    }

    @Test
    @DisplayName("An unmapped exception is 500, logged at ERROR with its stack trace, and its message is not returned")
    void unmappedExceptionIs500AndLogged() {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/users/7");
        RuntimeException boom = new RuntimeException("connection pool exhausted");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleGenericException(boom, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message())
                .isEqualTo("An unexpected error occurred")
                .doesNotContain("connection pool");

        assertThat(logEvents.list).as("a 500 with no log line is undiagnosable").hasSize(1);
        ILoggingEvent event = logEvents.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage()).contains("DELETE").contains("/api/v1/users/7");
        assertThat(event.getThrowableProxy()).as("stack trace attached").isNotNull();
        assertThat(event.getThrowableProxy().getMessage()).isEqualTo("connection pool exhausted");
    }

    @Test
    @DisplayName("A RuntimeException whose message looks like an auth failure is still a 500: no message sniffing")
    void noMessageSniffing() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleGenericException(new RuntimeException("Account is disabled"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
