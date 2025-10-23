package com.study.synopsi.repository;

import com.study.synopsi.model.Summary;
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
public interface SummaryRepository extends JpaRepository<Summary, Long> {

    /**
     * Find default (non-personalized) summary for an article
     */
    Optional<Summary> findByArticleIdAndUserIsNullAndSummaryTypeAndStatus(
            Long articleId,
            Summary.SummaryType summaryType,
            Summary.SummaryStatus status
    );

    /**
     * Find user-specific summary for an article
     */
    Optional<Summary> findByArticleIdAndUserIdAndSummaryTypeAndStatus(
            Long articleId,
            Long userId,
            Summary.SummaryType summaryType,
            Summary.SummaryStatus status
    );

    /**
     * Find all summaries for a specific article
     */
    List<Summary> findByArticleId(Long articleId);

    /**
     * Find all summaries created by/for a specific user
     */
    Page<Summary> findByUserId(Long userId, Pageable pageable);

    /**
     * Find summaries by status
     */
    List<Summary> findByStatus(Summary.SummaryStatus status);

    /**
     * Count summaries for an article
     */
    long countByArticleId(Long articleId);

    /**
     * Count user-specific summaries
     */
    long countByUserId(Long userId);

    /**
     * Check if default summary exists for article and type
     */
    boolean existsByArticleIdAndUserIsNullAndSummaryType(
            Long articleId,
            Summary.SummaryType summaryType
    );

    /**
     * Check if user-specific summary exists
     */
    boolean existsByArticleIdAndUserIdAndSummaryType(
            Long articleId,
            Long userId,
            Summary.SummaryType summaryType
    );

    /**
     * Find summaries older than a certain date (for cleanup)
     */
    @Query("SELECT s FROM Summary s WHERE s.createdAt < :cutoffDate AND s.status = :status")
    List<Summary> findOldSummaries(
            @Param("cutoffDate") LocalDateTime cutoffDate,
            @Param("status") Summary.SummaryStatus status
    );

    /**
     * Delete old summaries (for scheduled cleanup)
     */
    @Modifying
    @Query("DELETE FROM Summary s WHERE s.createdAt < :cutoffDate AND s.status = :status")
    int deleteOldSummaries(
            @Param("cutoffDate") LocalDateTime cutoffDate,
            @Param("status") Summary.SummaryStatus status
    );
}