package com.study.synopsi.repository;

import com.study.synopsi.model.UserArticleFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserArticleFeedbackRepository extends JpaRepository<UserArticleFeedback, Long> {

    /**
     * Find specific feedback entry for user, article, and feedback type
     */
    Optional<UserArticleFeedback> findByUserIdAndArticleIdAndFeedbackType(
            Long userId,
            Long articleId,
            UserArticleFeedback.FeedbackType feedbackType
    );

    /**
     * Find all feedback from a user for an article
     */
    List<UserArticleFeedback> findByUserIdAndArticleId(Long userId, Long articleId);

    /**
     * Find all feedback by a user
     */
    Page<UserArticleFeedback> findByUserId(Long userId, Pageable pageable);

    /**
     * Find all feedback for an article
     */
    List<UserArticleFeedback> findByArticleId(Long articleId);

    /**
     * Find feedback by type for a user
     */
    List<UserArticleFeedback> findByUserIdAndFeedbackType(
            Long userId,
            UserArticleFeedback.FeedbackType feedbackType
    );

    /**
     * Find all saved articles for a user
     */
    List<UserArticleFeedback> findByUserIdAndFeedbackTypeOrderByCreatedAtDesc(
            Long userId,
            UserArticleFeedback.FeedbackType feedbackType
    );

    /**
     * Check if user has given specific feedback on an article
     */
    boolean existsByUserIdAndArticleIdAndFeedbackType(
            Long userId,
            Long articleId,
            UserArticleFeedback.FeedbackType feedbackType
    );

    /**
     * Count feedback of a specific type for an article
     */
    long countByArticleIdAndFeedbackType(
            Long articleId,
            UserArticleFeedback.FeedbackType feedbackType
    );

    /**
     * Count total feedback for an article
     */
    long countByArticleId(Long articleId);

    /**
     * Get articles liked by user
     */
    @Query("SELECT f.article.id FROM UserArticleFeedback f " +
            "WHERE f.user.id = :userId AND f.feedbackType = 'LIKED'")
    List<Long> findLikedArticleIds(@Param("userId") Long userId);

    /**
     * Get articles saved by user
     */
    @Query("SELECT f.article.id FROM UserArticleFeedback f " +
            "WHERE f.user.id = :userId AND f.feedbackType = 'SAVED'")
    List<Long> findSavedArticleIds(@Param("userId") Long userId);

    /**
     * Get articles disliked by user
     */
    @Query("SELECT f.article.id FROM UserArticleFeedback f " +
            "WHERE f.user.id = :userId AND f.feedbackType = 'DISLIKED'")
    List<Long> findDislikedArticleIds(@Param("userId") Long userId);

    /**
     * Find feedback with ratings
     */
    @Query("SELECT f FROM UserArticleFeedback f WHERE f.user.id = :userId " +
            "AND f.rating IS NOT NULL ORDER BY f.rating DESC")
    List<UserArticleFeedback> findRatedFeedbackByUser(@Param("userId") Long userId);

    /**
     * Get average rating by user
     */
    @Query("SELECT AVG(f.rating) FROM UserArticleFeedback f " +
            "WHERE f.user.id = :userId AND f.rating IS NOT NULL")
    Double getAverageRatingByUser(@Param("userId") Long userId);

    /**
     * Get average rating for an article
     */
    @Query("SELECT AVG(f.rating) FROM UserArticleFeedback f " +
            "WHERE f.article.id = :articleId AND f.rating IS NOT NULL")
    Double getAverageRatingForArticle(@Param("articleId") Long articleId);

    /**
     * Find feedback with comments
     */
    @Query("SELECT f FROM UserArticleFeedback f WHERE f.user.id = :userId " +
            "AND f.comment IS NOT NULL AND f.comment != ''")
    List<UserArticleFeedback> findFeedbackWithCommentsByUser(@Param("userId") Long userId);

    /**
     * Get most liked articles (trending)
     */
    @Query("SELECT f.article.id, COUNT(f) as likeCount FROM UserArticleFeedback f " +
            "WHERE f.feedbackType = 'LIKED' AND f.createdAt >= :since " +
            "GROUP BY f.article.id " +
            "ORDER BY likeCount DESC")
    List<Object[]> getMostLikedArticlesSince(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Get most saved articles
     */
    @Query("SELECT f.article.id, COUNT(f) as saveCount FROM UserArticleFeedback f " +
            "WHERE f.feedbackType = 'SAVED' " +
            "GROUP BY f.article.id " +
            "ORDER BY saveCount DESC")
    List<Object[]> getMostSavedArticles(Pageable pageable);

    /**
     * Find reported articles requiring moderation
     */
    @Query("SELECT f FROM UserArticleFeedback f WHERE f.feedbackType = 'REPORTED' " +
            "ORDER BY f.createdAt DESC")
    List<UserArticleFeedback> findReportedArticles();

    /**
     * Count reports for an article
     */
    @Query("SELECT COUNT(f) FROM UserArticleFeedback f " +
            "WHERE f.article.id = :articleId AND f.feedbackType = 'REPORTED'")
    long countReportsForArticle(@Param("articleId") Long articleId);

    /**
     * Get feedback metrics for an article (likes, dislikes, saves, etc.)
     */
    @Query("SELECT f.feedbackType, COUNT(f) FROM UserArticleFeedback f " +
            "WHERE f.article.id = :articleId " +
            "GROUP BY f.feedbackType")
    List<Object[]> getFeedbackMetricsForArticle(@Param("articleId") Long articleId);

    /**
     * Find user's feedback on articles from specific topics
     */
    @Query("SELECT f FROM UserArticleFeedback f " +
            "JOIN f.article.articleTopics at " +
            "WHERE f.user.id = :userId AND at.topic.id = :topicId")
    List<UserArticleFeedback> findFeedbackByUserAndTopic(
            @Param("userId") Long userId,
            @Param("topicId") Long topicId
    );

    /**
     * Delete user's feedback on an article
     */
    void deleteByUserIdAndArticleId(Long userId, Long articleId);

    /**
     * Delete specific feedback entry
     */
    void deleteByUserIdAndArticleIdAndFeedbackType(
            Long userId,
            Long articleId,
            UserArticleFeedback.FeedbackType feedbackType
    );

    /**
     * Delete all feedback by a user (GDPR compliance)
     */
    void deleteByUserId(Long userId);

    /**
     * Update feedback rating
     */
    @Modifying
    @Query("UPDATE UserArticleFeedback f SET f.rating = :rating " +
            "WHERE f.user.id = :userId AND f.article.id = :articleId " +
            "AND f.feedbackType = :feedbackType")
    int updateRating(
            @Param("userId") Long userId,
            @Param("articleId") Long articleId,
            @Param("feedbackType") UserArticleFeedback.FeedbackType feedbackType,
            @Param("rating") Integer rating
    );

    /**
     * Find users who liked articles similar to what this user liked (collaborative filtering)
     */
    @Query("SELECT f2.user.id, COUNT(f2) as commonLikes " +
            "FROM UserArticleFeedback f1 " +
            "JOIN UserArticleFeedback f2 ON f1.article.id = f2.article.id " +
            "WHERE f1.user.id = :userId AND f2.user.id != :userId " +
            "AND f1.feedbackType = 'LIKED' AND f2.feedbackType = 'LIKED' " +
            "GROUP BY f2.user.id " +
            "HAVING COUNT(f2) >= :minCommonLikes " +
            "ORDER BY commonLikes DESC")
    List<Object[]> findUsersWithSimilarLikes(
            @Param("userId") Long userId,
            @Param("minCommonLikes") Long minCommonLikes
    );
}