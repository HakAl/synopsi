package com.study.synopsi.service;

import com.study.synopsi.model.User;
import com.study.synopsi.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkerAccountSeedService")
class WorkerAccountSeedServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private WorkerAccountSeedService service(String username, String password, String email) {
        return new WorkerAccountSeedService(userRepository, passwordEncoder, username, password, email);
    }

    @Test
    @DisplayName("Does nothing when no worker credentials are configured")
    void doesNothingWhenNotConfigured() {
        boolean created = service("", "", "").seedWorkerAccount();

        assertThat(created).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Does nothing when only the username is configured")
    void doesNothingWhenPasswordMissing() {
        boolean created = service("worker", "", "").seedWorkerAccount();

        assertThat(created).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Creates the account with an encoded password and a derived email")
    void createsAccountWithEncodedPassword() {
        when(userRepository.findByUsername("worker")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("worker@worker.synopsi.local")).thenReturn(false);
        when(passwordEncoder.encode("secret-pass")).thenReturn("encoded");

        boolean created = service("worker", "secret-pass", "").seedWorkerAccount();

        assertThat(created).isTrue();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("worker");
        assertThat(saved.getEmail()).isEqualTo("worker@worker.synopsi.local");
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getRole()).isEqualTo(User.UserRole.WORKER);
        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.getAccountLocked()).isFalse();
    }

    @Test
    @DisplayName("Uses the configured email when one is given")
    void usesConfiguredEmail() {
        when(userRepository.findByUsername("worker")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("ops@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        service("worker", "secret-pass", "ops@example.com").seedWorkerAccount();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("ops@example.com");
    }

    @Test
    @DisplayName("Is idempotent: an existing WORKER account is left untouched")
    void skipsWhenWorkerAccountAlreadyExists() {
        User existing = new User();
        existing.setUsername("worker");
        existing.setPassword("old-hash");
        existing.setRole(User.UserRole.WORKER);
        when(userRepository.findByUsername("worker")).thenReturn(Optional.of(existing));

        boolean created = service("worker", "secret-pass", "").seedWorkerAccount();

        assertThat(created).isFalse();
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    private User existingAccount(User.UserRole role) {
        User existing = new User();
        existing.setUsername("worker");
        existing.setPassword("old-hash");
        existing.setRole(role);
        return existing;
    }

    @Test
    @DisplayName("Promotes a legacy USER account to WORKER when the configured password verifies against it")
    void promotesLegacyUserAccountWhenPasswordMatches() {
        User existing = existingAccount(User.UserRole.USER);
        when(userRepository.findByUsername("worker")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("secret-pass", "old-hash")).thenReturn(true);

        boolean created = service("worker", "secret-pass", "").seedWorkerAccount();

        assertThat(created).isFalse();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        assertThat(captor.getValue().getRole()).isEqualTo(User.UserRole.WORKER);
        assertThat(captor.getValue().getPassword()).isEqualTo("old-hash");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Refuses to promote a USER whose password does not match the configured one, and fails startup")
    void refusesToPromoteOnPasswordMismatch() {
        User existing = existingAccount(User.UserRole.USER);
        when(userRepository.findByUsername("worker")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("secret-pass", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> service("worker", "secret-pass", "").seedWorkerAccount())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not match");

        assertThat(existing.getRole()).isEqualTo(User.UserRole.USER);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Never rewrites an ADMIN or MODERATOR to WORKER, even with a matching password")
    void refusesToPromoteNonUserRoles() {
        User existing = existingAccount(User.UserRole.ADMIN);
        when(userRepository.findByUsername("worker")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("secret-pass", "old-hash")).thenReturn(true);

        assertThatThrownBy(() -> service("worker", "secret-pass", "").seedWorkerAccount())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("role ADMIN");

        assertThat(existing.getRole()).isEqualTo(User.UserRole.ADMIN);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Refuses to seed when the derived email is already taken")
    void skipsWhenEmailTaken() {
        when(userRepository.findByUsername("worker")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("worker@worker.synopsi.local")).thenReturn(true);

        boolean created = service("worker", "secret-pass", "").seedWorkerAccount();

        assertThat(created).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Treats a whitespace-only username as not configured")
    void treatsWhitespaceUsernameAsUnset() {
        boolean created = service("   ", "secret-pass", "").seedWorkerAccount();

        assertThat(created).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rejects a username with surrounding whitespace, which the workers would send verbatim")
    void rejectsSurroundingWhitespace() {
        assertThatThrownBy(() -> service(" worker ", "secret-pass", "").seedWorkerAccount())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("surrounding whitespace");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tolerates a concurrent seed when the same worker identity exists after the failed insert")
    void toleratesConcurrentInsert() {
        User winner = existingAccount(User.UserRole.WORKER);
        when(userRepository.findByUsername("worker")).thenReturn(Optional.empty()).thenReturn(Optional.of(winner));
        when(userRepository.existsByEmail("worker@worker.synopsi.local")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(passwordEncoder.matches("secret-pass", "old-hash")).thenReturn(true);
        doThrow(new DataIntegrityViolationException("duplicate key")).when(userRepository).save(any(User.class));

        boolean created = service("worker", "secret-pass", "").seedWorkerAccount();

        assertThat(created).isFalse();
    }

    @Test
    @DisplayName("Fails startup when the account that won the insert race is not the worker identity")
    void rejectsConcurrentWinnerThatIsNotTheWorker() {
        User winner = existingAccount(User.UserRole.USER);
        when(userRepository.findByUsername("worker")).thenReturn(Optional.empty()).thenReturn(Optional.of(winner));
        when(userRepository.existsByEmail("worker@worker.synopsi.local")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        doThrow(new DataIntegrityViolationException("duplicate key")).when(userRepository).save(any(User.class));

        assertThatThrownBy(() -> service("worker", "secret-pass", "").seedWorkerAccount())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not the configured worker identity");
        assertThat(winner.getRole()).isEqualTo(User.UserRole.USER);
    }

    @Test
    @DisplayName("Fails startup when the concurrent winner is a WORKER with a different password")
    void rejectsConcurrentWorkerWithDifferentPassword() {
        User winner = existingAccount(User.UserRole.WORKER);
        when(userRepository.findByUsername("worker")).thenReturn(Optional.empty()).thenReturn(Optional.of(winner));
        when(userRepository.existsByEmail("worker@worker.synopsi.local")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(passwordEncoder.matches("secret-pass", "old-hash")).thenReturn(false);
        doThrow(new DataIntegrityViolationException("duplicate key")).when(userRepository).save(any(User.class));

        assertThatThrownBy(() -> service("worker", "secret-pass", "").seedWorkerAccount())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not the configured worker identity");
    }

    @Test
    @DisplayName("Fails startup when the insert fails and no account with that username exists")
    void propagatesIntegrityFailureWithoutWinner() {
        when(userRepository.findByUsername("worker")).thenReturn(Optional.empty()).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("worker@worker.synopsi.local")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        doThrow(new DataIntegrityViolationException("email collision")).when(userRepository).save(any(User.class));

        assertThatThrownBy(() -> service("worker", "secret-pass", "").seedWorkerAccount())
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("ApplicationRunner.run seeds the account")
    void runSeedsTheAccount() throws Exception {
        when(userRepository.findByUsername("worker")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("worker@worker.synopsi.local")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        service("worker", "secret-pass", "").run(mock(ApplicationArguments.class));

        verify(userRepository).save(any(User.class));
    }
}
