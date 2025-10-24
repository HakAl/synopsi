package com.study.synopsi.repository;

import com.study.synopsi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username (for authentication)
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email (for authentication and password reset)
     */
    Optional<User> findByEmail(String email);

    /**
     * Find User by password reset token
     *
     * @param resetToken
     * @return
     */
    Optional<User> findByResetToken(String resetToken);

    /**
     * Find user by username or email (flexible login)
     */
    @Query("SELECT u FROM User u WHERE u.username = :identifier OR u.email = :identifier")
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);

    /**
     * Check if username exists (for registration validation)
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists (for registration validation)
     */
    boolean existsByEmail(String email);

    /**
     * Find all users by role
     */
    List<User> findByRole(User.UserRole role);

    /**
     * Find enabled users only
     */
    List<User> findByEnabledTrue();

    /**
     * Find disabled users
     */
    List<User> findByEnabledFalse();

    /**
     * Find locked accounts
     */
    List<User> findByAccountLockedTrue();

    /**
     * Find users created after a specific date
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find users who haven't logged in since a specific date (inactive users)
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :cutoffDate OR u.lastLoginAt IS NULL")
    List<User> findInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Update last login timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("loginTime") LocalDateTime loginTime);

    /**
     * Lock/unlock user account
     */
    @Modifying
    @Query("UPDATE User u SET u.accountLocked = :locked WHERE u.id = :userId")
    void setAccountLocked(@Param("userId") Long userId, @Param("locked") boolean locked);

    /**
     * Enable/disable user account
     */
    @Modifying
    @Query("UPDATE User u SET u.enabled = :enabled WHERE u.id = :userId")
    void setEnabled(@Param("userId") Long userId, @Param("enabled") boolean enabled);

    /**
     * Count users by role
     */
    long countByRole(User.UserRole role);

    /**
     * Count active users (enabled and not locked)
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true AND u.accountLocked = false")
    long countActiveUsers();

    /**
     * Find users with reading history for a specific article
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.readingHistory rh WHERE rh.article.id = :articleId")
    List<User> findUsersWhoReadArticle(@Param("articleId") Long articleId);

    /**
     * Search users by username or email (for admin search)
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchUsers(@Param("searchTerm") String searchTerm);
}