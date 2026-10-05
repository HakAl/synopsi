package com.study.synopsi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.synopsi.config.JwtAuthenticationFilter;
import com.study.synopsi.config.JwtUtil;
import com.study.synopsi.dto.PasswordChangeDto;
import com.study.synopsi.dto.UserRequestDto;
import com.study.synopsi.dto.UserResponseDto;
import com.study.synopsi.exception.InvalidRequestException;
import com.study.synopsi.exception.UserNotFoundException;
import com.study.synopsi.service.AuthService;
import com.study.synopsi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private AuthService authService;

    @MockBean
    private AuthenticationManager authenticationManager;

    private UserRequestDto userRequestDto;
    private UserResponseDto userResponseDto;

    @BeforeEach
    void setUp() {
        // Initialize common test objects
        userRequestDto = UserRequestDto.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .build();
        
        userResponseDto = UserResponseDto.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();
    }

    @Test
    void createUser_whenValidInput_shouldReturnCreated() throws Exception {
        // Arrange
        given(userService.createUser(any(UserRequestDto.class)))
                .willReturn(userResponseDto);

        // Act
        ResultActions response = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequestDto)));

        // Assert
        response.andDo(print()) // Print the request and response for debugging
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is(userResponseDto.getUsername())))
                .andExpect(jsonPath("$.email", is(userResponseDto.getEmail())));
    }
    
    @Test
    void createUser_whenInvalidInput_shouldReturnBadRequest() throws Exception {
        // Arrange: Create a DTO with an invalid email and short password to trigger validation
        UserRequestDto invalidRequest = UserRequestDto.builder()
                .username("t") // Invalid size
                .email("not-an-email") // Invalid format
                .password("123") // Invalid size
                .build();
                
        // Act
        ResultActions response = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)));
        
        // Assert
        response.andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserById_whenUserExists_shouldReturnUser() throws Exception {
        // Arrange
        Long userId = 1L;
        given(userService.getUserById(userId)).willReturn(userResponseDto);

        // Act
        ResultActions response = mockMvc.perform(get("/api/v1/users/{id}", userId));

        // Assert
        response.andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is(userResponseDto.getUsername())));
    }
    
    @Test
    void getUserById_whenUserNotFound_shouldReturnNotFound() throws Exception {
        // Arrange
        Long userId = 1L;
        given(userService.getUserById(userId)).willThrow(new UserNotFoundException(userId));

        // Act
        ResultActions response = mockMvc.perform(get("/api/v1/users/{id}", userId));

        // Assert
        response.andExpect(status().isNotFound())
                .andDo(print())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("User not found: 1")));
    }

    @Test
    void changePassword_whenCurrentPasswordIsWrong_shouldReturnBadRequestNotUnauthorized() throws Exception {
        // A 401 here would log the user out (api.js redirects on 401 from
        // non-auth endpoints) and a 500 would be retried three times.
        Long userId = 1L;
        PasswordChangeDto dto = new PasswordChangeDto("wrong-password", "newpassword123");
        doThrow(new InvalidRequestException("Current password is incorrect"))
                .when(userService).changePassword(eq(userId), any(PasswordChangeDto.class));

        ResultActions response = mockMvc.perform(put("/api/v1/users/{id}/password", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)));

        response.andExpect(status().isBadRequest())
                .andDo(print())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Current password is incorrect")));
    }

    @Test
    void deleteUser_whenUserExists_shouldReturnNoContent() throws Exception {
        // Arrange
        Long userId = 1L;
        // No need to mock the return value of a void method if no exception is thrown.
        // Mockito will do nothing by default.
        
        // Act
        ResultActions response = mockMvc.perform(delete("/api/v1/users/{id}", userId));

        // Assert
        response.andExpect(status().isNoContent())
                .andDo(print());
    }
    
    @Test
    void deleteUser_whenServiceFailsUnexpectedly_shouldReturnInternalServerErrorWithoutInternalMessage() throws Exception {
        // Arrange: a plain RuntimeException is a bug, not a client error.
        // The controller used to catch it locally and answer 400 with the raw message.
        Long userId = 1L;
        doThrow(new RuntimeException("connection pool exhausted")).when(userService).deleteUser(userId);

        // Act
        ResultActions response = mockMvc.perform(delete("/api/v1/users/{id}", userId));

        // Assert
        response.andExpect(status().isInternalServerError())
                .andDo(print())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred")));
    }
}
