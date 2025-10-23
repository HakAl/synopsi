package com.study.synopsi.service;

import com.study.synopsi.exception.ArticleNotFoundException;
import com.study.synopsi.exception.SummaryJobNotFoundException;
import com.study.synopsi.exception.SummaryNotFoundException;
import com.study.synopsi.model.Article;
import com.study.synopsi.model.Summary;
import com.study.synopsi.model.SummaryJob;
import com.study.synopsi.model.User;
import com.study.synopsi.repository.ArticleRepository;
import com.study.synopsi.repository.SummaryJobRepository;
import com.study.synopsi.repository.SummaryRepository;
import com.study.synopsi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryService {

    private final SummaryRepository summaryRepository;
    private final SummaryJobRepository summaryJobRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    // Configuration constants
    private static final int JOB_CLEANUP_DAYS = 7;
    private static final int STALE_JOB_TIMEOUT_HOURS = 2;

    /**
     * Request a new summary for an article
     * Creates a job and submits to worker (currently stubbed)
     */
    @Transactional
    public SummaryJob requestSummary(
            Long articleId,
            Long userId,
            Summary.SummaryType summaryType,
            Summary.SummaryLength summaryLength) {

        // Validate article exists
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ArticleNotFoundException(articleId));

        // Validate user if provided
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        }

        // Check if summary already exists
        if (summaryExists(articleId, userId, summaryType)) {
            log.info("Summary already exists for article {} and user {}", articleId, userId);
            // You could either return existing or allow regeneration
            // For now, we'll allow duplicate requests (they become regenerations)
        }

        // Check if there's already a pending job
        List<SummaryJob.JobStatus> activeStatuses = Arrays.asList(
                SummaryJob.JobStatus.QUEUED,
                SummaryJob.JobStatus.PROCESSING
        );

        boolean jobExists = userId != null
                ? summaryJobRepository.existsByArticleIdAndUserIdAndStatusIn(articleId, userId, activeStatuses)
                : summaryJobRepository.existsByArticleIdAndUserIsNullAndStatusIn(articleId, activeStatuses);

        if (jobExists) {
            log.warn("Active job already exists for article {} and user {}", articleId, userId);
            throw new RuntimeException("Summary generation already in progress");
        }

        // Create new job
        SummaryJob job = new SummaryJob();
        job.setArticle(article);
        job.setUser(user);
        job.setSummaryType(summaryType);
        job.setSummaryLength(summaryLength);
        job.setStatus(SummaryJob.JobStatus.QUEUED);
        job.setPriority(userId != null ? 7 : 5); // User-specific summaries get higher priority
        job.setSubmittedAt(LocalDateTime.now());

        SummaryJob savedJob = summaryJobRepository.save(job);
        log.info("Created summary job {} for article {}", savedJob.getId(), articleId);

        // Update article status
        article.setStatus(Article.ArticleStatus.PROCESSING);
        articleRepository.save(article);

        // Submit to worker (stubbed for now)
        submitToWorker(savedJob);

        return savedJob;
    }

    /**
     * Get existing summary for an article
     * Returns user-specific summary if available, otherwise default
     */
    @Transactional(readOnly = true)
    public Optional<Summary> getSummary(
            Long articleId,
            Long userId,
            Summary.SummaryType summaryType) {

        // Try to find user-specific summary first
        if (userId != null) {
            Optional<Summary> userSummary = summaryRepository.findByArticleIdAndUserIdAndSummaryTypeAndStatus(
                    articleId, userId, summaryType, Summary.SummaryStatus.COMPLETED
            );
            if (userSummary.isPresent()) {
                return userSummary;
            }
        }

        // Fall back to default summary
        return summaryRepository.findByArticleIdAndUserIsNullAndSummaryTypeAndStatus(
                articleId, summaryType, Summary.SummaryStatus.COMPLETED
        );
    }

    /**
     * Get default (non-personalized) summary for an article
     */
    @Transactional(readOnly = true)
    public Optional<Summary> getDefaultSummary(Long articleId, Summary.SummaryType summaryType) {
        return summaryRepository.findByArticleIdAndUserIsNullAndSummaryTypeAndStatus(
                articleId, summaryType, Summary.SummaryStatus.COMPLETED
        );
    }

    /**
     * Get summary by ID
     */
    @Transactional(readOnly = true)
    public Summary getSummaryById(Long summaryId) {
        return summaryRepository.findById(summaryId)
                .orElseThrow(() -> new SummaryNotFoundException(summaryId));
    }

    /**
     * Get all summaries for a user
     */
    @Transactional(readOnly = true)
    public Page<Summary> getUserSummaries(Long userId, Pageable pageable) {
        return summaryRepository.findByUserId(userId, pageable);
    }

    /**
     * Get all summaries for an article
     */
    @Transactional(readOnly = true)
    public List<Summary> getArticleSummaries(Long articleId) {
        return summaryRepository.findByArticleId(articleId);
    }

    /**
     * Regenerate an existing summary
     */
    @Transactional
    public SummaryJob regenerateSummary(Long summaryId) {
        Summary existingSummary = getSummaryById(summaryId);
        
        // Create new job with same parameters
        SummaryJob job = requestSummary(
                existingSummary.getArticle().getId(),
                existingSummary.getUser() != null ? existingSummary.getUser().getId() : null,
                existingSummary.getSummaryType(),
                existingSummary.getSummaryLength()
        );

        // Mark existing summary as being regenerated
        existingSummary.incrementRegenerationCount();
        summaryRepository.save(existingSummary);

        log.info("Regenerating summary {} with new job {}", summaryId, job.getId());
        return job;
    }

    /**
     * Get job by ID
     */
    @Transactional(readOnly = true)
    public SummaryJob getJobById(Long jobId) {
        return summaryJobRepository.findById(jobId)
                .orElseThrow(() -> new SummaryJobNotFoundException(jobId));
    }

    /**
     * Get queued jobs ordered by priority
     */
    @Transactional(readOnly = true)
    public List<SummaryJob> getQueuedJobs() {
        return summaryJobRepository.findQueuedJobsByPriority();
    }

    /**
     * Submit job to worker (STUBBED - will be implemented when worker exists)
     * For now, just marks the job as processing
     */
    private void submitToWorker(SummaryJob job) {
        log.info("STUB: Would submit job {} to Python worker", job.getId());
        
        // TODO: When worker exists, implement actual submission:
        // 1. Call worker API endpoint (REST/gRPC/message queue)
        // 2. Store workerJobId in job
        // 3. Worker will callback when complete
        
        // For now, just log
        log.debug("Job details - Article: {}, Type: {}, Length: {}", 
                job.getArticle().getId(), 
                job.getSummaryType(), 
                job.getSummaryLength());
    }

    /**
     * Handle callback from worker when summary is complete
     * This will be called by worker webhook/callback
     */
    @Transactional
    public void handleWorkerCallback(Long jobId, String summaryText, String modelVersion, Integer tokenCount) {
        SummaryJob job = getJobById(jobId);
        
        try {
            // Create summary entity
            Summary summary = new Summary();
            summary.setArticle(job.getArticle());
            summary.setUser(job.getUser());
            summary.setSummaryText(summaryText);
            summary.setSummaryType(job.getSummaryType());
            summary.setSummaryLength(job.getSummaryLength());
            summary.setModelVersion(modelVersion);
            summary.setGeneratedAt(LocalDateTime.now());
            summary.setTokenCount(tokenCount);
            summary.setStatus(Summary.SummaryStatus.COMPLETED);

            Summary savedSummary = summaryRepository.save(summary);
            log.info("Created summary {} from job {}", savedSummary.getId(), jobId);

            // Mark job as completed
            job.markAsCompleted();
            summaryJobRepository.save(job);

            // Update article status if this was the default summary
            if (job.isDefaultSummaryJob()) {
                Article article = job.getArticle();
                article.setStatus(Article.ArticleStatus.SUMMARIZED);
                articleRepository.save(article);
            }

        } catch (Exception e) {
            log.error("Failed to process worker callback for job {}", jobId, e);
            job.markAsFailed("Failed to save summary: " + e.getMessage());
            summaryJobRepository.save(job);
        }
    }

    /**
     * Handle worker failure callback
     */
    @Transactional
    public void handleWorkerFailure(Long jobId, String errorMessage) {
        SummaryJob job = getJobById(jobId);
        job.incrementAttempts();

        if (job.canRetry()) {
            log.warn("Job {} failed but can retry. Attempt {}/{}", jobId, job.getAttempts(), job.getMaxAttempts());
            job.setStatus(SummaryJob.JobStatus.QUEUED);
            job.setErrorMessage(errorMessage + " (will retry)");
            summaryJobRepository.save(job);
            
            // Resubmit to worker
            submitToWorker(job);
        } else {
            log.error("Job {} failed permanently after {} attempts", jobId, job.getAttempts());
            job.markAsFailed(errorMessage);
            summaryJobRepository.save(job);

            // Update article status
            if (job.isDefaultSummaryJob()) {
                Article article = job.getArticle();
                article.setStatus(Article.ArticleStatus.FAILED);
                articleRepository.save(article);
            }
        }
    }

    /**
     * Check if summary exists
     */
    @Transactional(readOnly = true)
    public boolean summaryExists(Long articleId, Long userId, Summary.SummaryType summaryType) {
        if (userId != null) {
            return summaryRepository.existsByArticleIdAndUserIdAndSummaryType(articleId, userId, summaryType);
        } else {
            return summaryRepository.existsByArticleIdAndUserIsNullAndSummaryType(articleId, summaryType);
        }
    }

    /**
     * Retry a failed job
     */
    @Transactional
    public SummaryJob retryFailedJob(Long jobId) {
        SummaryJob job = getJobById(jobId);
        
        if (job.getStatus() != SummaryJob.JobStatus.FAILED) {
            throw new IllegalStateException("Can only retry FAILED jobs");
        }

        if (!job.canRetry()) {
            throw new IllegalStateException("Job has exceeded max retry attempts");
        }

        job.setStatus(SummaryJob.JobStatus.QUEUED);
        job.setErrorMessage(null);
        SummaryJob savedJob = summaryJobRepository.save(job);
        
        submitToWorker(savedJob);
        log.info("Retrying failed job {}", jobId);
        
        return savedJob;
    }

    /**
     * Scheduled cleanup of old completed jobs (runs daily at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldJobs() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(JOB_CLEANUP_DAYS);
        int deletedCount = summaryJobRepository.deleteOldCompletedJobs(cutoffDate);
        log.info("Cleaned up {} completed jobs older than {} days", deletedCount, JOB_CLEANUP_DAYS);
    }

    /**
     * Scheduled check for stale processing jobs (runs every hour)
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void checkStaleJobs() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(STALE_JOB_TIMEOUT_HOURS);
        List<SummaryJob> staleJobs = summaryJobRepository.findStaleProcessingJobs(cutoffTime);
        
        for (SummaryJob job : staleJobs) {
            log.warn("Found stale job {} stuck in PROCESSING state", job.getId());
            handleWorkerFailure(job.getId(), "Job timeout - worker did not respond");
        }
    }

    /**
     * Get job statistics
     */
    @Transactional(readOnly = true)
    public JobStatistics getJobStatistics() {
        JobStatistics stats = new JobStatistics();
        stats.setQueuedCount(summaryJobRepository.countByStatus(SummaryJob.JobStatus.QUEUED));
        stats.setProcessingCount(summaryJobRepository.countByStatus(SummaryJob.JobStatus.PROCESSING));
        stats.setCompletedCount(summaryJobRepository.countByStatus(SummaryJob.JobStatus.COMPLETED));
        stats.setFailedCount(summaryJobRepository.countByStatus(SummaryJob.JobStatus.FAILED));
        return stats;
    }

    // Inner class for statistics
    public static class JobStatistics {
        private long queuedCount;
        private long processingCount;
        private long completedCount;
        private long failedCount;

        // Getters and setters
        public long getQueuedCount() { return queuedCount; }
        public void setQueuedCount(long queuedCount) { this.queuedCount = queuedCount; }
        public long getProcessingCount() { return processingCount; }
        public void setProcessingCount(long processingCount) { this.processingCount = processingCount; }
        public long getCompletedCount() { return completedCount; }
        public void setCompletedCount(long completedCount) { this.completedCount = completedCount; }
        public long getFailedCount() { return failedCount; }
        public void setFailedCount(long failedCount) { this.failedCount = failedCount; }
    }
}