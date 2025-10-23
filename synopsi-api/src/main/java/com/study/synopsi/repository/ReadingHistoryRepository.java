package com.study.synopsi.repository;

import com.study.synopsi.model.ReadingHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, Long> {

    /**
     * Find reading history entry for a specific user and article
     */
    Optional<ReadingHistory> findByUserIdAndArticleId(Long userId, Long articleId);

    /**
     * Check if user has read an article
     */
    boolean existsByUserIdAndArticleId(Long userId, Long articleId);

    /**
     * Find all articles read by a user
     */
    Page<ReadingHistory> findByUserId(Long userId, Pageable pageable);

    /**
     * Find all users who read a specific article
     */
    List<ReadingHistory> findByArticleId(Long articleId);

    /**
     * Find user's reading history within a date range
     */
    @Query("SELECT rh FROM ReadingHistory rh WHERE rh.user.id = :userId " +
            "AND rh.readAt BETWEEN :startDate AND :endDate")
    List<ReadingHistory> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find user's most recently read articles
     */
    List<ReadingHistory> findByUserIdOrderByReadAtDesc(Long userId, Pageable pageable);

    /**
     * Find articles with high completion rate for a user
     */
    @Query("SELECT rh FROM ReadingHistory rh WHERE rh.user.id = :userId " +
            "AND rh.completionPercentage >= :minCompletion")
    List<ReadingHistory> findHighCompletionArticles(
            @Param("userId") Long userId,
            @Param("minCompletion") Integer minCompletion
    );

    /**
     * Find articles user spent significant time reading
     */
    @Query("SELECT rh FROM ReadingHistory rh WHERE rh.user.id = :userId " +
            "AND rh.timeSpentSeconds >= :minSeconds")
    List<ReadingHistory> findArticlesWithMinReadTime(
            @Param("userId") Long userId,
            @Param("minSeconds") Integer minSeconds
    );

    /**
     * Find frequently re-read articles by user
     */
    @Query("SELECT rh FROM ReadingHistory rh WHERE rh.user.id = :userId " +
            "AND rh.accessCount >= :minAccessCount ORDER BY rh.accessCount DESC")
    List<ReadingHistory> findFrequentlyAccessedArticles(
            @Param("userId") Long userId,
            @Param("minAccessCount") Integer minAccessCount
    );

    /**
     * Get total reading time for a user
     */
    @Query("SELECT COALESCE(SUM(rh.timeSpentSeconds), 0) FROM ReadingHistory rh " +
            "WHERE rh.user.id = :userId")
    Long getTotalReadingTimeSeconds(@Param("userId") Long userId);

    /**
     * Get average completion percentage for a user
     */
    @Query("SELECT AVG(rh.completionPercentage) FROM ReadingHistory rh " +
            "WHERE rh.user.id = :userId AND rh.completionPercentage IS NOT NULL")
    Double getAverageCompletionPercentage(@Param("userId") Long userId);

    /**
     * Count articles read by user
     */
    long countByUserId(Long userId);

    /**
     * Count times an article has been read
     */
    long countByArticleId(Long articleId);

    /**
     * Find user's reading activity for specific topics
     */
    @Query("SELECT rh FROM ReadingHistory rh " +
            "JOIN rh.article.articleTopics at " +
            "WHERE rh.user.id = :userId AND at.topic.id = :topicId")
    List<ReadingHistory> findByUserIdAndTopicId(
            @Param("userId") Long userId,
            @Param("topicId") Long topicId
    );

    /**
     * Get user's most read topics (for interest inference)
     */
    @Query("SELECT at.topic.id, at.topic.name, COUNT(rh) as readCount " +
            "FROM ReadingHistory rh " +
            "JOIN rh.article.articleTopics at " +
            "WHERE rh.user.id = :userId " +
            "GROUP BY at.topic.id, at.topic.name " +
            "ORDER BY readCount DESC")
    List<Object[]> getMostReadTopicsByUser(@Param("userId") Long userId);

    /**
     * Delete old reading history entries (for GDPR/cleanup)
     */
    void deleteByReadAtBefore(LocalDateTime cutoffDate);

    /**
     * Find inactive users (no reading history since date)
     */
    @Query("SELECT DISTINCT rh.user.id FROM ReadingHistory rh " +
            "WHERE rh.readAt < :cutoffDate")
    List<Long> findUserIdsWithNoRecentActivity(@Param("cutoffDate") LocalDateTime cutoffDate);
}