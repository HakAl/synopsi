package com.study.synopsi.service;

import com.study.synopsi.config.AuthenticatedUser;
import com.study.synopsi.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AccessControlService")
class AccessControlServiceTest {

    private final AccessControlService accessControl = new AccessControlService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private AuthenticatedUser login(Long id, User.UserRole role) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setPassword("hash");
        user.setEnabled(true);
        user.setAccountLocked(false);
        user.setRole(role);
        AuthenticatedUser principal = new AuthenticatedUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        return principal;
    }

    @Test
    @DisplayName("The caller may act on their own id")
    void selfIsAllowed() {
        login(7L, User.UserRole.USER);
        assertThatCode(() -> accessControl.requireSelfOrAdmin(7L)).doesNotThrowAnyException();
        assertThatCode(() -> accessControl.requireSelfOrAdminIfPresent(7L)).doesNotThrowAnyException();
        assertThat(accessControl.currentUser().getId()).isEqualTo(7L);
        assertThat(accessControl.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("Another user's id is denied, including a null id")
    void otherIsDenied() {
        login(7L, User.UserRole.USER);
        assertThatThrownBy(() -> accessControl.requireSelfOrAdmin(8L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(AccessControlService.FORBIDDEN_MESSAGE);
        assertThatThrownBy(() -> accessControl.requireSelfOrAdmin(null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(accessControl::requireAdmin)
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("An absent optional id needs no check")
    void absentOptionalIdIsAllowed() {
        login(7L, User.UserRole.USER);
        assertThatCode(() -> accessControl.requireSelfOrAdminIfPresent(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("An admin may act on anyone")
    void adminIsAllowed() {
        login(1L, User.UserRole.ADMIN);
        assertThatCode(() -> accessControl.requireSelfOrAdmin(8L)).doesNotThrowAnyException();
        assertThatCode(accessControl::requireAdmin).doesNotThrowAnyException();
        assertThat(accessControl.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("A moderator is not an admin")
    void moderatorIsNotAdmin() {
        login(2L, User.UserRole.MODERATOR);
        assertThatThrownBy(() -> accessControl.requireSelfOrAdmin(8L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(accessControl::requireAdmin).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("No authentication, or an anonymous one, is a missing-credentials failure not a denial")
    void noPrincipalIsUnauthenticated() {
        assertThatThrownBy(() -> accessControl.requireSelfOrAdmin(7L))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThatThrownBy(() -> accessControl.requireSelfOrAdmin(7L))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(accessControl::requireAdmin)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @DisplayName("Worker routes admit the worker and admins only")
    void workerRoutesAdmitWorkerAndAdmin() {
        login(3L, User.UserRole.WORKER);
        assertThatCode(accessControl::requireWorker).doesNotThrowAnyException();
        assertThatThrownBy(accessControl::requireAdmin).isInstanceOf(AccessDeniedException.class);

        login(1L, User.UserRole.ADMIN);
        assertThatCode(accessControl::requireWorker).doesNotThrowAnyException();

        login(7L, User.UserRole.USER);
        assertThatThrownBy(accessControl::requireWorker)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(AccessControlService.FORBIDDEN_MESSAGE);

        login(2L, User.UserRole.MODERATOR);
        assertThatThrownBy(accessControl::requireWorker).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("canActFor: shared resources for everyone, owned ones for the owner or an admin")
    void canActForFollowsOwnership() {
        login(7L, User.UserRole.USER);
        assertThat(accessControl.canActFor(null)).as("shared").isTrue();
        assertThat(accessControl.canActFor(7L)).as("own").isTrue();
        assertThat(accessControl.canActFor(8L)).as("foreign").isFalse();

        login(1L, User.UserRole.ADMIN);
        assertThat(accessControl.canActFor(8L)).as("admin").isTrue();

        login(3L, User.UserRole.WORKER);
        assertThat(accessControl.canActFor(8L)).as("the worker is not an owner").isFalse();

        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> accessControl.canActFor(8L))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @DisplayName("The principal exposes the role as a Spring authority")
    void principalCarriesRoleAuthority() {
        AuthenticatedUser admin = login(1L, User.UserRole.ADMIN);
        assertThat(admin.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        assertThat(admin.isEnabled()).isTrue();
        assertThat(admin.isAccountNonLocked()).isTrue();
    }
}
