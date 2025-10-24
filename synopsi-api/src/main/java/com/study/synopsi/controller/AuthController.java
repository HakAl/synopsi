package com.study.synopsi.controller;

import com.study.synopsi.dto.*;
import com.study.synopsi.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Register a new user account
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponseDto> register(@Valid @RequestBody UserRequestDto request) {
        log.info("POST /api/auth/register - Registering user: {}", request.getUsername());
        LoginResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     * Authenticate user and return JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        log.info("POST /api/auth/login - Login attempt for: {}", request.getUsernameOrEmail());
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/password-reset
     * Request password reset (simplified - just validates email)
     */
    @PostMapping("/password-reset")
    public ResponseEntity<MessageResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDto request) {
        
        log.info("POST /api/auth/password-reset - Request for: {}", request.getEmail());
        authService.requestPasswordReset(request);
        
        return ResponseEntity.ok(new MessageResponse(
                "If an account exists with that email, password reset instructions have been sent"
        ));
    }

    /**
     * POST /api/auth/password-reset/confirm
     * Confirm password reset with token and new password
     */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<MessageResponse> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmDto request) {

        log.info("POST /api/auth/password-reset/confirm - Confirming password reset");
        authService.confirmPasswordReset(request);

        return ResponseEntity.ok(new MessageResponse(
                "Password has been reset successfully. You can now login with your new password."
        ));
    }

    /**
     * Simple message response
     */
    record MessageResponse(String message) {}
}