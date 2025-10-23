package com.study.synopsi.repository;

import com.study.synopsi.model.SummaryJob;
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
public interface SummaryJobRepository extends JpaRepository<SummaryJob, Long> {

    /**
     * Find job by worker job ID
     */
    Optional<SummaryJob> findByWorkerJobId(String workerJobId);

    /**
     * Find jobs by status
     */
    List<SummaryJob> findByStatus(SummaryJob.JobStatus status);

    /**
     * Find queued jobs ordered by priority (higher first) and submission time
     */
    @Query("SELECT j FROM SummaryJob j WHERE j.status = 'QUEUED' ORDER BY j.priority DESC, j.submittedAt ASC")
    List<SummaryJob> findQueuedJobsByPriority();

    /**
     * Find jobs for a specific article
     */
    List<SummaryJob> findByArticleId(Long articleId);

    /**
     * Find jobs for a specific user
     */
    Page<SummaryJob> findByUserId(Long userId, Pageable pageable);

    /**
     * Find failed jobs that can be retried
     */
    @Query("SELECT j FROM SummaryJob j WHERE j.status = 'FAILED' AND j.attempts < j.maxAttempts")
    List<SummaryJob> findRetryableJobs();

    /**
     * Find stale processing jobs (stuck in PROCESSING state)
     */
    @Query("SELECT j FROM SummaryJob j WHERE j.status = 'PROCESSING' AND j.startedAt < :cutoffTime")
    List<SummaryJob> findStaleProcessingJobs(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Count jobs by status
     */
    long countByStatus(SummaryJob.JobStatus status);

    /**
     * Check if there's already a queued/processing job for article and user
     */
    boolean existsByArticleIdAndUserIdAndStatusIn(
            Long articleId,
            Long userId,
            List<SummaryJob.JobStatus> statuses
    );

    /**
     * Check if there's already a queued/processing default job for article
     */
    boolean existsByArticleIdAndUserIsNullAndStatusIn(
            Long articleId,
            List<SummaryJob.JobStatus> statuses
    );

    /**
     * Find old completed jobs for cleanup (completed > X days ago)
     */
    @Query("SELECT j FROM SummaryJob j WHERE j.status = 'COMPLETED' AND j.completedAt < :cutoffDate")
    List<SummaryJob> findOldCompletedJobs(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Delete old completed jobs
     */
    @Modifying
    @Query("DELETE FROM SummaryJob j WHERE j.status = 'COMPLETED' AND j.completedAt < :cutoffDate")
    int deleteOldCompletedJobs(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find jobs submitted in a time range (for monitoring/analytics)
     */
    @Query("SELECT j FROM SummaryJob j WHERE j.submittedAt BETWEEN :startDate AND :endDate")
    List<SummaryJob> findJobsInTimeRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}