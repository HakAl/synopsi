package com.study.synopsi.service;

import com.study.synopsi.config.JwtUtil;
import com.study.synopsi.dto.*;
import com.study.synopsi.model.User;
import com.study.synopsi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
@Slf4j
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    // Use @Lazy on AuthenticationManager to break circular dependency
    public AuthService(
            UserRepository userRepository,
            UserService userService,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            @Lazy AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Register a new user
     */
    @Transactional
    public LoginResponseDto register(UserRequestDto request) {
        log.info("Registering new user: {}", request.getUsername());

        // Create user through UserService
        UserResponseDto userResponse = userService.createUser(request);

        // Generate JWT token
        String token = jwtUtil.generateToken(request.getUsername(), userResponse.getId());

        log.info("User registered successfully: {}", request.getUsername());

        return LoginResponseDto.builder()
                .token(token)
                .type("Bearer")
                .userId(userResponse.getId())
                .username(userResponse.getUsername())
                .email(userResponse.getEmail())
                .message("Registration successful")
                .build();
    }

    /**
     * Authenticate user and return JWT token
     */
    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        log.info("Login attempt for: {}", request.getUsernameOrEmail());

        // Find user by username or email
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new RuntimeException("Invalid username/email or password"));

        // Check if account is enabled
        if (!user.getEnabled()) {
            throw new RuntimeException("Account is disabled");
        }

        // Check if account is locked
        if (user.getAccountLocked()) {
            throw new RuntimeException("Account is locked");
        }

        // Authenticate
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        request.getPassword()
                )
        );

        // Update last login
        userService.updateLastLogin(user.getId());

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());

        log.info("Login successful for user: {}", user.getUsername());

        return LoginResponseDto.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .message("Login successful")
                .build();
    }

    /**
     * Initiate password reset - generates token and logs it
     */
    @Transactional
    public void requestPasswordReset(PasswordResetRequestDto request) {
        log.info("Password reset requested for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // Don't reveal if email exists or not for security
        if (user == null) {
            log.info("Password reset requested for non-existent email: {}", request.getEmail());
            return;
        }

        // Generate reset token (UUID)
        String resetToken = UUID.randomUUID().toString();

        // Set expiration (1 hour from now)
        LocalDateTime expiry = LocalDateTime.now().plusHours(1);

        user.setResetToken(resetToken);
        user.setResetTokenExpiry(expiry);
        userRepository.save(user);

        // LOG THE TOKEN FOR DEVELOPMENT (in production, send email)
        log.warn("=".repeat(80));
        log.warn("PASSWORD RESET TOKEN FOR: {}", request.getEmail());
        log.warn("Token: {}", resetToken);
        log.warn("Expires: {}", expiry);
        log.warn("Reset URL: http://localhost:8080/reset-password.html?token={}", resetToken);
        log.warn("=".repeat(80));
    }

    /**
     * Confirm password reset with token
     */
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmDto request) {
        log.info("Password reset confirmation attempt with token: {}", request.getToken());

        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        // Check if token is expired
        if (user.getResetTokenExpiry() == null ||
                user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token has expired");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // Clear reset token
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userRepository.save(user);

        log.info("Password reset successful for user: {}", user.getUsername());
    }

    /**
     * Load user by username for Spring Security
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getEnabled(),
                true, // accountNonExpired
                true, // credentialsNonExpired
                !user.getAccountLocked(), // accountNonLocked
                new ArrayList<>() // authorities (empty since no roles)
        );
    }
}