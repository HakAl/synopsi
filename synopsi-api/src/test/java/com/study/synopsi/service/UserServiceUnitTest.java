package com.study.synopsi.service;

import com.study.synopsi.dto.PasswordChangeDto;
import com.study.synopsi.dto.UserRequestDto;
import com.study.synopsi.dto.UserResponseDto;
import com.study.synopsi.dto.UserUpdateDto;
import com.study.synopsi.model.User;
import com.study.synopsi.repository.ReadingHistoryRepository;
import com.study.synopsi.repository.UserPreferenceRepository;
import com.study.synopsi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private ReadingHistoryRepository readingHistoryRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks // Creates an instance of UserService and injects the mocks into it
    private UserService userService;

    private User user;
    private UserRequestDto userRequestDto;

    @BeforeEach
    void setUp() {
        // Common setup for tests
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedPassword");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(User.UserRole.USER);
        user.setEnabled(true);
        user.setAccountLocked(false);

        userRequestDto = UserRequestDto.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .build();
    }

    @Test
    void createUser_whenEmailIsUnique_shouldCreateUser() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        UserResponseDto createdUser = userService.createUser(userRequestDto);

        // Assert
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getUsername()).isEqualTo(userRequestDto.getUsername());
        assertThat(createdUser.getEmail()).isEqualTo(userRequestDto.getEmail());
        verify(userRepository, times(1)).save(any(User.class)); // Verify save was called once
    }

    @Test
    void createUser_whenEmailExists_shouldThrowException() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createUser(userRequestDto);
        });

        assertThat(exception.getMessage()).isEqualTo("Email already exists: " + userRequestDto.getEmail());
        verify(userRepository, never()).save(any(User.class)); // Verify save was never called
    }

    @Test
    void getUserById_whenUserExists_shouldReturnUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        UserResponseDto foundUser = userService.getUserById(1L);

        // Assert
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(1L);
    }

    @Test
    void getUserById_whenUserDoesNotExist_shouldThrowException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userService.getUserById(1L);
        });
    }

    @Test
    void updateUser_whenEmailIsUnchanged_shouldUpdateProfile() {
        // Arrange
        UserUpdateDto updateDto = UserUpdateDto.builder()
                .firstName("UpdatedFirst")
                .lastName("UpdatedLast")
                .email(user.getEmail()) // Email is the same
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        userService.updateUser(1L, updateDto);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
        verify(userRepository, never()).existsByEmail(anyString()); // Email check should be skipped
        assertThat(user.getFirstName()).isEqualTo("UpdatedFirst");
    }

    @Test
    void changePassword_whenCurrentPasswordIsCorrect_shouldChangePassword() {
        // Arrange
        PasswordChangeDto passwordDto = new PasswordChangeDto("oldPassword", "newPassword");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newHashedPassword");

        // Act
        userService.changePassword(1L, passwordDto);

        // Assert
        verify(userRepository, times(1)).save(user);
        assertThat(user.getPassword()).isEqualTo("newHashedPassword");
    }

    @Test
    void changePassword_whenCurrentPasswordIsIncorrect_shouldThrowException() {
        // Arrange
        PasswordChangeDto passwordDto = new PasswordChangeDto("wrongOldPassword", "newPassword");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongOldPassword", "hashedPassword")).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userService.changePassword(1L, passwordDto);
        });
        verify(userRepository, never()).save(any(User.class));
    }
}