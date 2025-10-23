package com.study.synopsi.controller;

import com.study.synopsi.dto.*;
import com.study.synopsi.model.User;
import com.study.synopsi.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * POST /api/v1/users
     * Create a new user
     */
    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto request) {
        log.info("POST /api/v1/users - Creating user with username: {}", request.getUsername());
        UserResponseDto response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/users/{id}
     * Get user by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        log.info("GET /api/v1/users/{}", id);
        UserResponseDto response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/users/{id}/stats
     * Get user with statistics
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<UserResponseDto> getUserWithStats(@PathVariable Long id) {
        log.info("GET /api/v1/users/{}/stats", id);
        UserResponseDto response = userService.getUserWithStats(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/users
     * Get all users with pagination
     */
    @GetMapping
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        
        log.info("GET /api/v1/users - page: {}, size: {}", 
                pageable.getPageNumber(), pageable.getPageSize());
        
        Page<UserResponseDto> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/v1/users/search
     * Search users by term
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserResponseDto>> searchUsers(@RequestParam String term) {
        log.info("GET /api/v1/users/search?term={}", term);
        List<UserResponseDto> users = userService.searchUsers(term);
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/v1/users/email/{email}
     * Get user by email
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponseDto> getUserByEmail(@PathVariable String email) {
        log.info("GET /api/v1/users/email/{}", email);
        UserResponseDto response = userService.getUserByEmail(email);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/users/username/{username}
     * Get user by username
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponseDto> getUserByUsername(@PathVariable String username) {
        log.info("GET /api/v1/users/username/{}", username);
        UserResponseDto response = userService.getUserByUsername(username);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/users/enabled
     * Get all enabled users
     */
    @GetMapping("/enabled")
    public ResponseEntity<List<UserResponseDto>> getEnabledUsers() {
        log.info("GET /api/v1/users/enabled");
        List<UserResponseDto> users = userService.getEnabledUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/v1/users/role/{role}
     * Get users by role
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserResponseDto>> getUsersByRole(@PathVariable User.UserRole role) {
        log.info("GET /api/v1/users/role/{}", role);
        List<UserResponseDto> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/v1/users/count/active
     * Get count of active users
     */
    @GetMapping("/count/active")
    public ResponseEntity<Long> getActiveUserCount() {
        log.info("GET /api/v1/users/count/active");
        long count = userService.getActiveUserCount();
        return ResponseEntity.ok(count);
    }

    /**
     * PUT /api/v1/users/{id}
     * Update user profile
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateDto updateDto) {
        
        log.info("PUT /api/v1/users/{}", id);
        UserResponseDto response = userService.updateUser(id, updateDto);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/v1/users/{id}/password
     * Change user password
     */
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordChangeDto passwordChangeDto) {
        
        log.info("PUT /api/v1/users/{}/password", id);
        userService.changePassword(id, passwordChangeDto);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/v1/users/{id}/enable
     * Enable user account
     */
    @PutMapping("/{id}/enable")
    public ResponseEntity<Void> enableUser(@PathVariable Long id) {
        log.info("PUT /api/v1/users/{}/enable", id);
        userService.enableUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/v1/users/{id}/disable
     * Disable user account
     */
    @PutMapping("/{id}/disable")
    public ResponseEntity<Void> disableUser(@PathVariable Long id) {
        log.info("PUT /api/v1/users/{}/disable", id);
        userService.disableUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/v1/users/{id}/lock
     * Lock user account
     */
    @PutMapping("/{id}/lock")
    public ResponseEntity<Void> lockUser(@PathVariable Long id) {
        log.info("PUT /api/v1/users/{}/lock", id);
        userService.lockUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/v1/users/{id}/unlock
     * Unlock user account
     */
    @PutMapping("/{id}/unlock")
    public ResponseEntity<Void> unlockUser(@PathVariable Long id) {
        log.info("PUT /api/v1/users/{}/unlock", id);
        userService.unlockUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/v1/users/{id}
     * Soft delete user (disable account)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("DELETE /api/v1/users/{}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/users/{id}/preferences
     * Get user's topic preferences
     */
    @GetMapping("/{id}/preferences")
    public ResponseEntity<List<UserPreferenceDto>> getUserPreferences(@PathVariable Long id) {
        log.info("GET /api/v1/users/{}/preferences", id);
        List<UserPreferenceDto> preferences = userService.getUserPreferences(id);
        return ResponseEntity.ok(preferences);
    }

    /**
     * GET /api/v1/users/check/email
     * Check if email exists
     */
    @GetMapping("/check/email")
    public ResponseEntity<Boolean> checkEmailExists(@RequestParam String email) {
        log.info("GET /api/v1/users/check/email?email={}", email);
        boolean exists = userService.emailExists(email);
        return ResponseEntity.ok(exists);
    }

    /**
     * GET /api/v1/users/check/username
     * Check if username exists
     */
    @GetMapping("/check/username")
    public ResponseEntity<Boolean> checkUsernameExists(@RequestParam String username) {
        log.info("GET /api/v1/users/check/username?username={}", username);
        boolean exists = userService.usernameExists(username);
        return ResponseEntity.ok(exists);
    }

    /**
     * Exception handler for this controller
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Error in UserController: {}", ex.getMessage(), ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Simple error response DTO
     */
    record ErrorResponse(int status, String message, long timestamp) {}
}