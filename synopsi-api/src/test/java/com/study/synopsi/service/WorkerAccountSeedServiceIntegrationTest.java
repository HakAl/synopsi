package com.study.synopsi.service;

import com.study.synopsi.config.JwtUtil;
import com.study.synopsi.dto.LoginRequestDto;
import com.study.synopsi.dto.LoginResponseDto;
import com.study.synopsi.model.User;
import com.study.synopsi.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression test for the worker login defect: docker-compose and the k8s
 * secret hand the workers a username and password, but nothing created that
 * account, so worker login failed on every fresh database.
 *
 * The API must seed the worker account from configuration on startup so the
 * credentials the workers are given actually exist.
 */
@SpringBootTest(properties = {
        "synopsi.worker.username=seeded-worker",
        "synopsi.worker.password=seeded-worker-pass"
})
@DisplayName("Worker account seeding")
class WorkerAccountSeedServiceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("Seeds the configured worker account on a fresh database")
    void seedsConfiguredWorkerAccountOnFreshDatabase() {
        User worker = userRepository.findByUsername("seeded-worker").orElse(null);

        assertThat(worker)
                .as("worker account configured via synopsi.worker.* should exist after startup")
                .isNotNull();
        assertThat(passwordEncoder.matches("seeded-worker-pass", worker.getPassword()))
                .as("stored password must be the encoded configured password")
                .isTrue();
        assertThat(worker.getRole()).isEqualTo(User.UserRole.WORKER);
        assertThat(worker.getEnabled()).isTrue();
        assertThat(worker.getAccountLocked()).isFalse();
    }

    @Test
    @DisplayName("Worker can log in with the configured credentials and receives a usable token")
    void workerCanLogInWithConfiguredCredentials() {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsernameOrEmail("seeded-worker");
        request.setPassword("seeded-worker-pass");

        LoginResponseDto response = authService.login(request);

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUsername()).isEqualTo("seeded-worker");
        assertThat(jwtUtil.validateToken(response.getToken(), "seeded-worker")).isTrue();
        assertThat(jwtUtil.extractUsername(response.getToken())).isEqualTo("seeded-worker");
    }

    @Test
    @DisplayName("Wrong password for the seeded worker is still rejected")
    void wrongPasswordIsRejected() {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsernameOrEmail("seeded-worker");
        request.setPassword("not-the-password");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
