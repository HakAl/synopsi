package com.study.synopsi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.synopsi.dto.UserRequestDto;
import com.study.synopsi.dto.UserResponseDto;
import com.study.synopsi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class) // Focus only on testing the UserController
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc; // Main entry point for server-side Spring MVC test support

    @MockitoBean // Creates a mock of UserService and adds it to the application context
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper; // Utility for converting Java objects to/from JSON

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
    void getUserById_whenUserNotFound_shouldReturnBadRequest() throws Exception {
        // Arrange
        Long userId = 1L;
        // Mock the service to throw the exception that the controller's handler will catch
        given(userService.getUserById(userId)).willThrow(new RuntimeException("User not found"));

        // Act
        ResultActions response = mockMvc.perform(get("/api/v1/users/{id}", userId));

        // Assert
        response.andExpect(status().isBadRequest())
                .andDo(print())
                .andExpect(jsonPath("$.message", is("User not found")));
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
    void deleteUser_whenServiceThrowsException_shouldReturnBadRequest() throws Exception {
        // Arrange
        Long userId = 1L;
        doThrow(new RuntimeException("Cannot delete user")).when(userService).deleteUser(userId);
        
        // Act
        ResultActions response = mockMvc.perform(delete("/api/v1/users/{id}", userId));

        // Assert
        response.andExpect(status().isBadRequest())
                .andDo(print())
                .andExpect(jsonPath("$.message", is("Cannot delete user")));
    }
}