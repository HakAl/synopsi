package com.study.synopsi.repository;

import com.study.synopsi.model.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    /**
     * Find specific user preference for a topic
     */
    Optional<UserPreference> findByUserIdAndTopicId(Long userId, Long topicId);

    /**
     * Find all preferences for a user
     */
    List<UserPreference> findByUserId(Long userId);

    /**
     * Find all active preferences for a user
     */
    List<UserPreference> findByUserIdAndIsActiveTrue(Long userId);

    /**
     * Find all users interested in a specific topic
     */
    List<UserPreference> findByTopicId(Long topicId);

    /**
     * Find users with high interest in a topic
     */
    @Query("SELECT up FROM UserPreference up WHERE up.topic.id = :topicId " +
            "AND up.interestLevel IN ('HIGH', 'VERY_HIGH') AND up.isActive = true")
    List<UserPreference> findHighInterestUsersByTopic(@Param("topicId") Long topicId);

    /**
     * Find user preferences by interest level
     */
    List<UserPreference> findByUserIdAndInterestLevel(
            Long userId,
            UserPreference.InterestLevel interestLevel
    );

    /**
     * Find active preferences with minimum interest level
     */
    @Query("SELECT up FROM UserPreference up WHERE up.user.id = :userId " +
            "AND up.isActive = true " +
            "AND (up.interestLevel = 'HIGH' OR up.interestLevel = 'VERY_HIGH')")
    List<UserPreference> findActiveHighInterestPreferences(@Param("userId") Long userId);

    /**
     * Check if user has preference for a topic
     */
    boolean existsByUserIdAndTopicId(Long userId, Long topicId);

    /**
     * Count active preferences for a user
     */
    long countByUserIdAndIsActiveTrue(Long userId);

    /**
     * Count users interested in a topic
     */
    long countByTopicIdAndIsActiveTrue(Long topicId);

    /**
     * Get all topic IDs user is interested in
     */
    @Query("SELECT up.topic.id FROM UserPreference up " +
            "WHERE up.user.id = :userId AND up.isActive = true")
    List<Long> findActiveTopicIdsByUserId(@Param("userId") Long userId);

    /**
     * Update interest level for a preference
     */
    @Modifying
    @Query("UPDATE UserPreference up SET up.interestLevel = :level " +
            "WHERE up.user.id = :userId AND up.topic.id = :topicId")
    int updateInterestLevel(
            @Param("userId") Long userId,
            @Param("topicId") Long topicId,
            @Param("level") UserPreference.InterestLevel level
    );

    /**
     * Deactivate a user preference
     */
    @Modifying
    @Query("UPDATE UserPreference up SET up.isActive = false " +
            "WHERE up.user.id = :userId AND up.topic.id = :topicId")
    int deactivatePreference(
            @Param("userId") Long userId,
            @Param("topicId") Long topicId
    );

    /**
     * Reactivate a user preference
     */
    @Modifying
    @Query("UPDATE UserPreference up SET up.isActive = true " +
            "WHERE up.user.id = :userId AND up.topic.id = :topicId")
    int reactivatePreference(
            @Param("userId") Long userId,
            @Param("topicId") Long topicId
    );

    /**
     * Delete all preferences for a user (GDPR compliance)
     */
    void deleteByUserId(Long userId);

    /**
     * Delete specific user preference
     */
    void deleteByUserIdAndTopicId(Long userId, Long topicId);

    /**
     * Get user preference distribution (for analytics)
     */
    @Query("SELECT up.interestLevel, COUNT(up) FROM UserPreference up " +
            "WHERE up.user.id = :userId AND up.isActive = true " +
            "GROUP BY up.interestLevel")
    List<Object[]> getInterestLevelDistribution(@Param("userId") Long userId);

    /**
     * Find users with similar preferences to a given user (for collaborative filtering)
     */
    @Query("SELECT up2.user.id, COUNT(up2) as commonTopics " +
            "FROM UserPreference up1 " +
            "JOIN UserPreference up2 ON up1.topic.id = up2.topic.id " +
            "WHERE up1.user.id = :userId AND up2.user.id != :userId " +
            "AND up1.isActive = true AND up2.isActive = true " +
            "GROUP BY up2.user.id " +
            "HAVING COUNT(up2) >= :minCommonTopics " +
            "ORDER BY commonTopics DESC")
    List<Object[]> findUsersWithSimilarPreferences(
            @Param("userId") Long userId,
            @Param("minCommonTopics") Long minCommonTopics
    );

    /**
     * Bulk update preferences active status
     */
    @Modifying
    @Query("UPDATE UserPreference up SET up.isActive = :isActive " +
            "WHERE up.user.id = :userId")
    int updateAllPreferencesActiveStatus(
            @Param("userId") Long userId,
            @Param("isActive") Boolean isActive
    );
}