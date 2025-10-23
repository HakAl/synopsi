package com.study.synopsi.controller;

import com.study.synopsi.model.Summary;
import com.study.synopsi.model.SummaryJob;
import com.study.synopsi.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/summaries")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    /**
     * Request a new summary for an article
     * POST /api/summaries/request
     */
    @PostMapping("/request")
    public ResponseEntity<SummaryJob> requestSummary(
            @RequestParam Long articleId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "BRIEF") Summary.SummaryType summaryType,
            @RequestParam(defaultValue = "MEDIUM") Summary.SummaryLength summaryLength) {

        SummaryJob job = summaryService.requestSummary(articleId, userId, summaryType, summaryLength);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
    }

    /**
     * Get summary for an article
     * GET /api/summaries/article/{articleId}
     */
    @GetMapping("/article/{articleId}")
    public ResponseEntity<Summary> getSummary(
            @PathVariable Long articleId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "BRIEF") Summary.SummaryType summaryType) {

        Optional<Summary> summary = summaryService.getSummary(articleId, userId, summaryType);
        return summary
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get default summary for an article
     * GET /api/summaries/article/{articleId}/default
     */
    @GetMapping("/article/{articleId}/default")
    public ResponseEntity<Summary> getDefaultSummary(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "BRIEF") Summary.SummaryType summaryType) {

        Optional<Summary> summary = summaryService.getDefaultSummary(articleId, summaryType);
        return summary
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get summary by ID
     * GET /api/summaries/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Summary> getSummaryById(@PathVariable Long id) {
        Summary summary = summaryService.getSummaryById(id);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get all summaries for an article
     * GET /api/summaries/article/{articleId}/all
     */
    @GetMapping("/article/{articleId}/all")
    public ResponseEntity<List<Summary>> getArticleSummaries(@PathVariable Long articleId) {
        List<Summary> summaries = summaryService.getArticleSummaries(articleId);
        return ResponseEntity.ok(summaries);
    }

    /**
     * Get all summaries for a user (paginated)
     * GET /api/summaries/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<Summary>> getUserSummaries(
            @PathVariable Long userId,
            Pageable pageable) {

        Page<Summary> summaries = summaryService.getUserSummaries(userId, pageable);
        return ResponseEntity.ok(summaries);
    }

    /**
     * Regenerate a summary
     * POST /api/summaries/{id}/regenerate
     */
    @PostMapping("/{id}/regenerate")
    public ResponseEntity<SummaryJob> regenerateSummary(@PathVariable Long id) {
        SummaryJob job = summaryService.regenerateSummary(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
    }

    /**
     * Get job status by ID
     * GET /api/summaries/jobs/{jobId}
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<SummaryJob> getJobById(@PathVariable Long jobId) {
        SummaryJob job = summaryService.getJobById(jobId);
        return ResponseEntity.ok(job);
    }

    /**
     * Get all queued jobs
     * GET /api/summaries/jobs/queued
     */
    @GetMapping("/jobs/queued")
    public ResponseEntity<List<SummaryJob>> getQueuedJobs() {
        List<SummaryJob> jobs = summaryService.getQueuedJobs();
        return ResponseEntity.ok(jobs);
    }

    /**
     * Retry a failed job
     * POST /api/summaries/jobs/{jobId}/retry
     */
    @PostMapping("/jobs/{jobId}/retry")
    public ResponseEntity<SummaryJob> retryFailedJob(@PathVariable Long jobId) {
        SummaryJob job = summaryService.retryFailedJob(jobId);
        return ResponseEntity.ok(job);
    }

    /**
     * Get job statistics
     * GET /api/summaries/jobs/statistics
     */
    @GetMapping("/jobs/statistics")
    public ResponseEntity<SummaryService.JobStatistics> getJobStatistics() {
        SummaryService.JobStatistics stats = summaryService.getJobStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Check if summary exists
     * GET /api/summaries/exists
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> summaryExists(
            @RequestParam Long articleId,
            @RequestParam(required = false) Long userId,
            @RequestParam Summary.SummaryType summaryType) {

        boolean exists = summaryService.summaryExists(articleId, userId, summaryType);
        return ResponseEntity.ok(exists);
    }

    /**
     * Worker callback endpoint (called by Python worker when summary is complete)
     * POST /api/summaries/callback/complete
     */
    @PostMapping("/callback/complete")
    public ResponseEntity<Void> handleWorkerCallback(
            @RequestParam Long jobId,
            @RequestParam String summaryText,
            @RequestParam String modelVersion,
            @RequestParam(required = false) Integer tokenCount) {

        summaryService.handleWorkerCallback(jobId, summaryText, modelVersion, tokenCount);
        return ResponseEntity.ok().build();
    }

    /**
     * Worker failure callback endpoint
     * POST /api/summaries/callback/failure
     */
    @PostMapping("/callback/failure")
    public ResponseEntity<Void> handleWorkerFailure(
            @RequestParam Long jobId,
            @RequestParam String errorMessage) {

        summaryService.handleWorkerFailure(jobId, errorMessage);
        return ResponseEntity.ok().build();
    }
}