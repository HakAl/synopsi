package com.study.synopsi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.synopsi.config.JwtAuthenticationFilter;
import com.study.synopsi.config.JwtUtil;
import com.study.synopsi.dto.*;
import com.study.synopsi.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private AuthenticationManager authenticationManager;

    private UserRequestDto registerRequest;
    private LoginRequestDto loginRequest;
    private LoginResponseDto loginResponse;
    private PasswordResetRequestDto passwordResetRequest;

    @BeforeEach
    void setUp() {
        registerRequest = UserRequestDto.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .build();

        loginRequest = LoginRequestDto.builder()
                .usernameOrEmail("testuser")
                .password("password123")
                .build();

        loginResponse = LoginResponseDto.builder()
                .token("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
                .type("Bearer")
                .userId(1L)
                .username("testuser")
                .email("testuser@example.com")
                .message("Login successful")
                .build();

        passwordResetRequest = PasswordResetRequestDto.builder()
                .email("testuser@example.com")
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should register successfully")
        void shouldRegisterSuccessfully() throws Exception {
            when(authService.register(any(UserRequestDto.class))).thenReturn(loginResponse);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.type", is("Bearer")));
        }

        @Test
        @DisplayName("Should return 400 when username missing")
        void shouldReturn400WhenUsernameMissing() throws Exception {
            registerRequest.setUsername(null);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully")
        void shouldLoginSuccessfully() throws Exception {
            when(authService.login(any(LoginRequestDto.class))).thenReturn(loginResponse);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists());
        }

        @Test
        @DisplayName("Should return 400 when password missing")
        void shouldReturn400WhenPasswordMissing() throws Exception {
            loginRequest.setPassword(null);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/password-reset")
    class PasswordResetTests {

        @Test
        @DisplayName("Should accept password reset request")
        void shouldAcceptPasswordResetRequest() throws Exception {
            doNothing().when(authService).requestPasswordReset(any(PasswordResetRequestDto.class));

            mockMvc.perform(post("/api/v1/auth/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordResetRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should return 400 when email invalid")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            passwordResetRequest.setEmail("not-an-email");

            mockMvc.perform(post("/api/v1/auth/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordResetRequest)))
                    .andExpect(status().isBadRequest());
        }
    }
}