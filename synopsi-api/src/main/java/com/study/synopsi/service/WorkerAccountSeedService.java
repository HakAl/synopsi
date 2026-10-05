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

import java.util.Optional;

/**
 * Seeds the service account the ingestion and summarization workers log in
 * with. The workers receive their credentials from the environment
 * (docker-compose, the Kubernetes secret, or a local .env), and this runner
 * makes sure the matching account exists in the API's database on startup.
 *
 * Nothing is seeded unless both {@code synopsi.worker.username} and
 * {@code synopsi.worker.password} are set, so no default credential ships
 * with the application. An existing WORKER account with the same username is
 * left untouched, including its password, so rotating the configured password
 * does not update an account that already exists.
 *
 * Accounts seeded by earlier versions were created as USER, and the worker
 * routes are now reserved for WORKER. Such an account is promoted on startup
 * only when the configured password verifies against its stored hash, which
 * is what establishes that it is the service account and not an unrelated
 * registration that happens to use the same name (registration is public and
 * lets the caller pick any free username). A password mismatch, or any role
 * other than USER, fails startup without modifying the row.
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
     * Creates the worker account if it is configured and does not already
     * exist, or promotes an existing account of that name to WORKER.
     *
     * @return true if an account was created
     */
    public boolean seedWorkerAccount() {
        if (username.isBlank() || password.isEmpty()) {
            log.info("No worker account configured (synopsi.worker.username / synopsi.worker.password). "
                    + "Set both to provision the WORKER account; a self-registered account is an "
                    + "ordinary USER and cannot use the summarization worker routes.");
            return false;
        }

        if (!username.equals(username.trim())) {
            throw new IllegalStateException("synopsi.worker.username has surrounding whitespace ('" + username
                    + "'). Workers send API_USERNAME verbatim, so this account could never log in.");
        }

        Optional<User> existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            reconcileExistingAccount(existing.get());
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
        worker.setRole(User.UserRole.WORKER);
        worker.setEnabled(true);
        worker.setAccountLocked(false);

        try {
            userRepository.save(worker);
        } catch (DataIntegrityViolationException e) {
            Optional<User> winner = userRepository.findByUsername(username);
            if (winner.isEmpty()) {
                throw new IllegalStateException("Failed to seed worker account '" + username
                        + "' and no account with that username exists", e);
            }
            // Another instance seeding the same configuration produces the same
            // identity; anything else that won the race is a collision.
            if (!isServiceAccount(winner.get())) {
                throw new IllegalStateException("Worker account '" + username + "' was created concurrently "
                        + "but is not the configured worker identity (role " + winner.get().getRole()
                        + ", password mismatch or wrong role). Refusing to continue.", e);
            }
            log.info("Worker account '{}' was created concurrently by another instance. Skipping seed.", username);
            return false;
        }
        log.info("Seeded worker account '{}'", username);
        return true;
    }

    /**
     * An account with the configured username already exists. It is left alone
     * when it is already the WORKER; a legacy USER is promoted only after the
     * configured password verifies against its stored hash; anything else is a
     * collision with an unrelated account and fails startup without a write.
     */
    private void reconcileExistingAccount(User account) {
        User.UserRole role = account.getRole();
        if (role == User.UserRole.WORKER) {
            log.info("Worker account '{}' already exists. Skipping seed.", username);
            return;
        }
        boolean passwordMatches = passwordEncoder.matches(password, account.getPassword());
        if (role == User.UserRole.USER && passwordMatches) {
            account.setRole(User.UserRole.WORKER);
            userRepository.save(account);
            log.info("Promoted existing worker account '{}' from role USER to WORKER", username);
            return;
        }
        if (!passwordMatches) {
            throw new IllegalStateException("An account named '" + username + "' (role " + role
                    + ") already exists and the configured synopsi.worker.password does not match its "
                    + "password. It is not the worker's account, or the password was rotated: choose "
                    + "another worker username, or reconcile the password through the API. The account "
                    + "was not modified.");
        }
        throw new IllegalStateException("An account named '" + username + "' already exists with role "
                + role + ". Only a USER account is promoted to WORKER. The account was not modified.");
    }

    private boolean isServiceAccount(User account) {
        return account.getRole() == User.UserRole.WORKER
                && passwordEncoder.matches(password, account.getPassword());
    }
}
