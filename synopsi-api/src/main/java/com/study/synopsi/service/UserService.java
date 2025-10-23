package com.study.synopsi.service;

import com.study.synopsi.dto.*;
import com.study.synopsi.model.User;
import com.study.synopsi.model.UserPreference;
import com.study.synopsi.repository.ReadingHistoryRepository;
import com.study.synopsi.repository.UserPreferenceRepository;
import com.study.synopsi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final ReadingHistoryRepository readingHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new user
     */
    @Transactional
    public UserResponseDto createUser(UserRequestDto request) {
        log.info("Creating new user with username: {}", request.getUsername());
        
        // Validate email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }
        
        // Note: Username doesn't need to be unique per requirements
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(User.UserRole.USER);
        user.setEnabled(true);
        user.setAccountLocked(false);
        
        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());
        
        return toUserResponseDto(savedUser);
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long userId) {
        log.info("Fetching user with ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return toUserResponseDto(user);
    }

    /**
     * Get user by email
     */
    @Transactional(readOnly = true)
    public UserResponseDto getUserByEmail(String email) {
        log.info("Fetching user with email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return toUserResponseDto(user);
    }

    /**
     * Get user by username
     */
    @Transactional(readOnly = true)
    public UserResponseDto getUserByUsername(String username) {
        log.info("Fetching user with username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return toUserResponseDto(user);
    }

    /**
     * Get all users with pagination
     */
    @Transactional(readOnly = true)
    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        log.info("Fetching all users - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable)
                .map(this::toUserResponseDto);
    }

    /**
     * Get all enabled users
     */
    @Transactional(readOnly = true)
    public List<UserResponseDto> getEnabledUsers() {
        log.info("Fetching all enabled users");
        return userRepository.findByEnabledTrue().stream()
                .map(this::toUserResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Search users by term (username, email, first name, last name)
     */
    @Transactional(readOnly = true)
    public List<UserResponseDto> searchUsers(String searchTerm) {
        log.info("Searching users with term: {}", searchTerm);
        return userRepository.searchUsers(searchTerm).stream()
                .map(this::toUserResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Update user profile
     */
    @Transactional
    public UserResponseDto updateUser(Long userId, UserUpdateDto updateDto) {
        log.info("Updating user with ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        // Update email if provided and changed
        if (updateDto.getEmail() != null && !updateDto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(updateDto.getEmail())) {
                throw new RuntimeException("Email already exists: " + updateDto.getEmail());
            }
            user.setEmail(updateDto.getEmail());
        }
        
        // Update other fields if provided
        if (updateDto.getFirstName() != null) {
            user.setFirstName(updateDto.getFirstName());
        }
        if (updateDto.getLastName() != null) {
            user.setLastName(updateDto.getLastName());
        }
        if (updateDto.getPreferences() != null) {
            user.setPreferences(updateDto.getPreferences());
        }
        
        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", userId);
        
        return toUserResponseDto(updatedUser);
    }

    /**
     * Change user password
     */
    @Transactional
    public void changePassword(Long userId, PasswordChangeDto passwordChangeDto) {
        log.info("Changing password for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        // Verify current password
        if (!passwordEncoder.matches(passwordChangeDto.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(passwordChangeDto.getNewPassword()));
        userRepository.save(user);
        
        log.info("Password changed successfully for user: {}", userId);
    }

    /**
     * Soft delete user (disable account)
     */
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Soft deleting user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        user.setEnabled(false);
        userRepository.save(user);
        
        log.info("User soft deleted: {}", userId);
    }

    /**
     * Enable user account
     */
    @Transactional
    public void enableUser(Long userId) {
        log.info("Enabling user: {}", userId);
        userRepository.setEnabled(userId, true);
        log.info("User enabled: {}", userId);
    }

    /**
     * Disable user account
     */
    @Transactional
    public void disableUser(Long userId) {
        log.info("Disabling user: {}", userId);
        userRepository.setEnabled(userId, false);
        log.info("User disabled: {}", userId);
    }

    /**
     * Lock user account
     */
    @Transactional
    public void lockUser(Long userId) {
        log.info("Locking user account: {}", userId);
        userRepository.setAccountLocked(userId, true);
        log.info("User account locked: {}", userId);
    }

    /**
     * Unlock user account
     */
    @Transactional
    public void unlockUser(Long userId) {
        log.info("Unlocking user account: {}", userId);
        userRepository.setAccountLocked(userId, false);
        log.info("User account unlocked: {}", userId);
    }

    /**
     * Update last login timestamp
     */
    @Transactional
    public void updateLastLogin(Long userId) {
        log.debug("Updating last login for user: {}", userId);
        userRepository.updateLastLogin(userId, LocalDateTime.now());
    }

    /**
     * Get user's topic preferences
     */
    @Transactional(readOnly = true)
    public List<UserPreferenceDto> getUserPreferences(Long userId) {
        log.info("Fetching preferences for user: {}", userId);
        
        // Verify user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        List<UserPreference> preferences = userPreferenceRepository.findByUserId(userId);
        return preferences.stream()
                .map(this::toUserPreferenceDto)
                .collect(Collectors.toList());
    }

    /**
     * Get user statistics
     */
    @Transactional(readOnly = true)
    public UserResponseDto getUserWithStats(Long userId) {
        log.info("Fetching user with statistics: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        UserResponseDto dto = toUserResponseDto(user);
        
        // Add statistics
        dto.setTotalArticlesRead(readingHistoryRepository.countByUserId(userId));
        dto.setTotalPreferences(userPreferenceRepository.countByUserIdAndIsActiveTrue(userId));
        
        return dto;
    }

    /**
     * Get count of active users
     */
    @Transactional(readOnly = true)
    public long getActiveUserCount() {
        return userRepository.countActiveUsers();
    }

    /**
     * Get users by role
     */
    @Transactional(readOnly = true)
    public List<UserResponseDto> getUsersByRole(User.UserRole role) {
        log.info("Fetching users with role: {}", role);
        return userRepository.findByRole(role).stream()
                .map(this::toUserResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Check if email exists
     */
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Check if username exists
     */
    @Transactional(readOnly = true)
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    // Mapper methods

    private UserResponseDto toUserResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().toString())
                .enabled(user.getEnabled())
                .accountLocked(user.getAccountLocked())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private UserPreferenceDto toUserPreferenceDto(UserPreference preference) {
        return UserPreferenceDto.builder()
                .id(preference.getId())
                .topicId(preference.getTopic().getId())
                .topicName(preference.getTopic().getName())
                .interestLevel(preference.getInterestLevel())
                .isActive(preference.getIsActive())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }
}