package com.study.synopsi.service;

import com.study.synopsi.config.JwtAuthenticationFilter;
import com.study.synopsi.config.JwtUtil;
import com.study.synopsi.dto.*;
import com.study.synopsi.model.User;
import com.study.synopsi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@DisplayName("AuthService Integration Tests")
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private AuthenticationManager authenticationManager;

    private UserRequestDto userRequest;

    @BeforeEach
    void setUp() {
        userRequest = UserRequestDto.builder()
                .username("testuser")
                .email("testuser@example.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .build();

        // Mock JWT token generation
        when(jwtUtil.generateToken(any(String.class), any(Long.class)))
                .thenReturn("mocked-jwt-token");
    }

    @Test
    @DisplayName("Should register new user and return JWT token")
    void shouldRegisterNewUserAndReturnToken() {
        // When
        LoginResponseDto response = authService.register(userRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked-jwt-token");
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("testuser@example.com");
        assertThat(response.getMessage()).isEqualTo("Registration successful");

        // Verify user was saved in database
        User savedUser = userRepository.findByEmail("testuser@example.com").orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should throw exception when registering with duplicate email")
    void shouldThrowExceptionWhenRegisteringWithDuplicateEmail() {
        // Given - create first user
        authService.register(userRequest);

        // When/Then - try to register with same email
        UserRequestDto duplicateRequest = UserRequestDto.builder()
                .username("anotheruser")
                .email("testuser@example.com")
                .password("password456")
                .build();

        assertThatThrownBy(() -> authService.register(duplicateRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    @DisplayName("Should login successfully with username")
    void shouldLoginSuccessfullyWithUsername() {
        // Given - create user first
        authService.register(userRequest);

        // Mock authentication
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .usernameOrEmail("testuser")
                .password("password123")
                .build();

        // When
        LoginResponseDto response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked-jwt-token");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getMessage()).isEqualTo("Login successful");
    }

    @Test
    @DisplayName("Should login successfully with email")
    void shouldLoginSuccessfullyWithEmail() {
        // Given - create user first
        authService.register(userRequest);

        // Mock authentication
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .usernameOrEmail("testuser@example.com")
                .password("password123")
                .build();

        // When
        LoginResponseDto response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked-jwt-token");
        assertThat(response.getEmail()).isEqualTo("testuser@example.com");
    }

    @Test
    @DisplayName("Should throw exception when login with invalid credentials")
    void shouldThrowExceptionWhenLoginWithInvalidCredentials() {
        // Given - create user first
        authService.register(userRequest);

        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .usernameOrEmail("testuser")
                .password("wrongpassword")
                .build();

        // Mock authentication failure
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When/Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Should throw exception when login with non-existent user")
    void shouldThrowExceptionWhenLoginWithNonExistentUser() {
        // Given
        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .usernameOrEmail("nonexistent")
                .password("password123")
                .build();

        // When/Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid username/email or password");
    }

    @Test
    @DisplayName("Should handle password reset request for existing email")
    void shouldHandlePasswordResetRequestForExistingEmail() {
        // Given - create user first
        authService.register(userRequest);

        PasswordResetRequestDto resetRequest = PasswordResetRequestDto.builder()
                .email("testuser@example.com")
                .build();

        // When/Then - should not throw exception
        authService.requestPasswordReset(resetRequest);
    }

    @Test
    @DisplayName("Should handle password reset request for non-existent email silently")
    void shouldHandlePasswordResetForNonExistentEmailSilently() {
        // Given
        PasswordResetRequestDto resetRequest = PasswordResetRequestDto.builder()
                .email("nonexistent@example.com")
                .build();

        // When - should complete without exception
        authService.requestPasswordReset(resetRequest);

        // Then - verify no user exists with that email
        assertThat(userRepository.findByEmail("nonexistent@example.com")).isEmpty();
    }

    @Test
    @DisplayName("Should load user by username for Spring Security")
    void shouldLoadUserByUsername() {
        // Given - create user first
        authService.register(userRequest);

        // When
        UserDetails userDetails = authService.loadUserByUsername("testuser");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when loading non-existent user")
    void shouldThrowExceptionWhenLoadingNonExistentUser() {
        // When/Then
        assertThatThrownBy(() -> authService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}