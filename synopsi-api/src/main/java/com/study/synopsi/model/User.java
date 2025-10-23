package com.study.synopsi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password; // Store BCrypt hashed password

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Boolean accountLocked = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime lastLoginAt;

    // User preferences/settings as JSON or individual columns
    @Column(columnDefinition = "TEXT")
    private String preferences; // Could store JSON for flexible settings

    // Bidirectional relationships - mapped by the owning side
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserPreference> userPreferences = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ReadingHistory> readingHistory = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserArticleFeedback> articleFeedback = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Summary> summaries = new HashSet<>();

    // Enum for user roles
    public enum UserRole {
        USER,
        ADMIN,
        MODERATOR
    }

    // Helper methods for managing relationships
    public void addUserPreference(UserPreference preference) {
        userPreferences.add(preference);
        preference.setUser(this);
    }

    public void removeUserPreference(UserPreference preference) {
        userPreferences.remove(preference);
        preference.setUser(null);
    }

    public void addReadingHistory(ReadingHistory history) {
        readingHistory.add(history);
        history.setUser(this);
    }

    public void addArticleFeedback(UserArticleFeedback feedback) {
        articleFeedback.add(feedback);
        feedback.setUser(this);
    }

    public void addSummary(Summary summary) {
        summaries.add(summary);
        summary.setUser(this);
    }

    public void removeSummary(Summary summary) {
        summaries.remove(summary);
        summary.setUser(null);
    }
}