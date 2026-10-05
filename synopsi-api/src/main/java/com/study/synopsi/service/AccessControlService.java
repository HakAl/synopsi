package com.study.synopsi.service;

import com.study.synopsi.config.AuthenticatedUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Ownership checks for user-scoped endpoints. The caller is the principal
 * JwtAuthenticationFilter put in the SecurityContext. Controllers call these
 * before touching the service layer, so a denied request does no work and
 * does not reveal whether the target exists.
 */
@Service
@Slf4j
public class AccessControlService {

    public static final String FORBIDDEN_MESSAGE = "You don't have permission to access this resource.";

    /**
     * The authenticated caller. Anonymous or missing authentication is a 401,
     * which only happens on a permitAll route or with the filter chain off.
     */
    public AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        return user;
    }

    public boolean isAdmin() {
        return currentUser().isAdmin();
    }

    /**
     * The caller must be the user identified by userId, or an admin.
     */
    public void requireSelfOrAdmin(Long userId) {
        AuthenticatedUser caller = currentUser();
        if (caller.isAdmin()) {
            return;
        }
        if (userId == null || !userId.equals(caller.getId())) {
            log.warn("User {} (id {}) denied access to resources of user {}",
                    caller.getUsername(), caller.getId(), userId);
            throw new AccessDeniedException(FORBIDDEN_MESSAGE);
        }
    }

    /**
     * For optional userId query parameters: absent means the shared,
     * user-independent resource and needs no ownership check.
     */
    public void requireSelfOrAdminIfPresent(Long userId) {
        if (userId != null) {
            requireSelfOrAdmin(userId);
        }
    }

    public void requireAdmin() {
        AuthenticatedUser caller = currentUser();
        if (!caller.isAdmin()) {
            log.warn("User {} (id {}) denied access to an admin-only resource",
                    caller.getUsername(), caller.getId());
            throw new AccessDeniedException(FORBIDDEN_MESSAGE);
        }
    }

    /**
     * The worker routes (queued-job list, completion and failure callbacks,
     * job statistics) act on every user's jobs. Only the seeded WORKER
     * account and admins may use them.
     */
    public void requireWorker() {
        AuthenticatedUser caller = currentUser();
        if (!caller.isWorker() && !caller.isAdmin()) {
            log.warn("User {} (id {}) denied access to a worker-only resource",
                    caller.getUsername(), caller.getId());
            throw new AccessDeniedException(FORBIDDEN_MESSAGE);
        }
    }

    /**
     * Whether the caller may act on a resource owned by ownerId. A null owner
     * is a shared resource (a default summary or job) and is visible to every
     * authenticated user. Services use this for opaque resource ids, where the
     * owner is only known after the row is loaded; a denied lookup is reported
     * as not found so the id space is not an existence oracle.
     */
    public boolean canActFor(Long ownerId) {
        if (ownerId == null) {
            return true;
        }
        AuthenticatedUser caller = currentUser();
        return caller.isAdmin() || ownerId.equals(caller.getId());
    }
}
