package com.study.synopsi.service;

import com.study.synopsi.config.JwtAuthenticationFilter;
import com.study.synopsi.config.JwtUtil;
import com.study.synopsi.dto.UserRequestDto;
import com.study.synopsi.dto.UserResponseDto;
import com.study.synopsi.dto.UserUpdateDto;
import com.study.synopsi.model.User;
import com.study.synopsi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    // Add these mocks for SecurityConfig dependencies
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Test
    void createUser_shouldSaveUserToDatabase() {
        // Arrange
        UserRequestDto request = UserRequestDto.builder()
                .username("integration_user")
                .email("integration@example.com")
                .password("password123")
                .firstName("Integration")
                .lastName("Test")
                .build();

        // Act
        UserResponseDto createdUserDto = userService.createUser(request);

        // Assert
        User foundUser = userRepository.findById(createdUserDto.getId()).orElse(null);
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getEmail()).isEqualTo("integration@example.com");
        assertThat(foundUser.getPassword()).isNotEqualTo("password123"); // Check if password was encoded
    }

    @Test
    void createUser_withDuplicateEmail_shouldFail() {
        // Arrange: Create an initial user
        UserRequestDto request1 = UserRequestDto.builder()
                .username("user1")
                .email("duplicate@example.com")
                .password("password123")
                .build();
        userService.createUser(request1);

        // Arrange: Prepare a request with the same email
        UserRequestDto request2 = UserRequestDto.builder()
                .username("user2")
                .email("duplicate@example.com")
                .password("password456")
                .build();

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createUser(request2);
        });
        assertThat(exception.getMessage()).contains("Email already exists");
    }
    
    @Test
    void updateUser_withNewUniqueEmail_shouldUpdateSuccessfully() {
        // Arrange
        UserRequestDto createRequest = UserRequestDto.builder()
                .username("updatable_user")
                .email("update@example.com")
                .password("password123")
                .build();
        UserResponseDto createdUser = userService.createUser(createRequest);
        
        UserUpdateDto updateRequest = UserUpdateDto.builder()
                .email("new_unique_email@example.com")
                .firstName("UpdatedName")
                .build();

        // Act
        UserResponseDto updatedUser = userService.updateUser(createdUser.getId(), updateRequest);

        // Assert
        assertThat(updatedUser.getEmail()).isEqualTo("new_unique_email@example.com");
        assertThat(updatedUser.getFirstName()).isEqualTo("UpdatedName");
    }

    @Test
    void deleteUser_shouldSetEnabledToFalse() {
        // Arrange
        UserRequestDto request = UserRequestDto.builder()
                .username("deletable_user")
                .email("delete@example.com")
                .password("password123")
                .build();
        UserResponseDto createdUser = userService.createUser(request);
        assertThat(createdUser.getEnabled()).isTrue();

        // Act
        userService.deleteUser(createdUser.getId());

        // Assert
        User deletedUser = userRepository.findById(createdUser.getId()).orElseThrow();
        assertThat(deletedUser.getEnabled()).isFalse();
    }
    
    @Test
    void searchUsers_shouldReturnMatchingUsers() {
        // Arrange
        userService.createUser(UserRequestDto.builder().username("john_doe").email("john.doe@test.com").password("p").firstName("John").lastName("Doe").build());
        userService.createUser(UserRequestDto.builder().username("jane_smith").email("jane.smith@test.com").password("p").firstName("Jane").lastName("Smith").build());
        userService.createUser(UserRequestDto.builder().username("another_guy").email("another@test.com").password("p").firstName("Johnathan").lastName("Crane").build());
        
        // Act
        List<UserResponseDto> results = userService.searchUsers("john");
        
        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).extracting(UserResponseDto::getUsername).containsExactlyInAnyOrder("john_doe", "another_guy");
    }
}