package com.study.synopsi.config;

import com.study.synopsi.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

/**
 * The authenticated principal. Carries the database id and role so that
 * controllers can compare a path or query userId against the caller without
 * another lookup. Spring's own UserDetails implementation only had the
 * username, which is why no ownership check existed before.
 */
public final class AuthenticatedUser implements UserDetails {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final User.UserRole role;

    public AuthenticatedUser(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.enabled = Boolean.TRUE.equals(user.getEnabled());
        this.accountNonLocked = !Boolean.TRUE.equals(user.getAccountLocked());
        this.role = user.getRole() != null ? user.getRole() : User.UserRole.USER;
    }

    public Long getId() {
        return id;
    }

    public User.UserRole getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role == User.UserRole.ADMIN;
    }

    public boolean isWorker() {
        return role == User.UserRole.WORKER;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return "AuthenticatedUser{id=" + id + ", username='" + username + "', role=" + role + "}";
    }
}
