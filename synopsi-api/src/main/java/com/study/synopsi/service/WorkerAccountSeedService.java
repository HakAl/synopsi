package com.study.synopsi.service;

import com.study.synopsi.model.User;
import com.study.synopsi.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Seeds the service account the ingestion and summarization workers log in
 * with. The workers receive their credentials from the environment
 * (docker-compose, the Kubernetes secret, or a local .env), and this runner
 * makes sure the matching account exists in the API's database on startup.
 *
 * Nothing is seeded unless both {@code synopsi.worker.username} and
 * {@code synopsi.worker.password} are set, so no default credential ships
 * with the application. An existing account with the same username is left
 * untouched, including its password: rotating the configured password does
 * not update an account that already exists.
 *
 * The single save runs in its own transaction. If another instance seeds the
 * same account first (multi-replica startup against a shared database), the
 * failed insert is confirmed by re-reading the username and startup continues.
 * Any other integrity failure, or a configured username with surrounding
 * whitespace (the workers send the value verbatim, so it could never log in),
 * fails startup so the misconfiguration is visible.
 */
@Service
@Slf4j
public class WorkerAccountSeedService implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String email;

    public WorkerAccountSeedService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${synopsi.worker.username:}") String username,
            @Value("${synopsi.worker.password:}") String password,
            @Value("${synopsi.worker.email:}") String email) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username == null ? "" : username;
        this.password = password == null ? "" : password;
        this.email = email == null ? "" : email.trim();
    }

    @Override
    public void run(ApplicationArguments args) {
        seedWorkerAccount();
    }

    /**
     * Creates the worker account if it is configured and does not already exist.
     *
     * @return true if an account was created
     */
    public boolean seedWorkerAccount() {
        if (username.isBlank() || password.isEmpty()) {
            log.info("No worker account configured (synopsi.worker.username / synopsi.worker.password). "
                    + "Workers must use an account registered through /api/v1/auth/register.");
            return false;
        }

        if (!username.equals(username.trim())) {
            throw new IllegalStateException("synopsi.worker.username has surrounding whitespace ('" + username
                    + "'). Workers send API_USERNAME verbatim, so this account could never log in.");
        }

        if (userRepository.existsByUsername(username)) {
            log.info("Worker account '{}' already exists. Skipping seed.", username);
            return false;
        }

        String workerEmail = email.isEmpty() ? username + "@worker.synopsi.local" : email;
        if (userRepository.existsByEmail(workerEmail)) {
            log.warn("Cannot seed worker account '{}': email '{}' is already in use.", username, workerEmail);
            return false;
        }

        User worker = new User();
        worker.setUsername(username);
        worker.setEmail(workerEmail);
        worker.setPassword(passwordEncoder.encode(password));
        worker.setFirstName("Synopsi");
        worker.setLastName("Worker");
        worker.setRole(User.UserRole.USER);
        worker.setEnabled(true);
        worker.setAccountLocked(false);

        try {
            userRepository.save(worker);
        } catch (DataIntegrityViolationException e) {
            if (userRepository.existsByUsername(username)) {
                log.info("Worker account '{}' was created concurrently by another instance. Skipping seed.", username);
                return false;
            }
            throw new IllegalStateException("Failed to seed worker account '" + username
                    + "' and no account with that username exists", e);
        }
        log.info("Seeded worker account '{}'", username);
        return true;
    }
}
