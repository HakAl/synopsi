package com.study.synopsi.controller;

import com.study.synopsi.dto.SummaryJobResponseDto;
import com.study.synopsi.dto.SummaryResponseDto;
import com.study.synopsi.model.Summary;
import com.study.synopsi.model.SummaryJob;
import com.study.synopsi.service.AccessControlService;
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
@RequestMapping("/api/v1/summaries")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;
    private final AccessControlService accessControl;

    /**
     * Request a new summary for an article
     * POST /api/v1/summaries/request
     */
    @PostMapping("/request")
    public ResponseEntity<SummaryJobResponseDto> requestSummary(
            @RequestParam Long articleId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "BRIEF") Summary.SummaryType summaryType,
            @RequestParam(defaultValue = "MEDIUM") Summary.SummaryLength summaryLength) {

        accessControl.requireSelfOrAdminIfPresent(userId);
        SummaryJobResponseDto job =
                summaryService.requestSummaryAsDto(articleId, userId, summaryType, summaryLength);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
    }

    /**
     * Get summary for an article
     * GET /api/v1/summaries/article/{articleId}
     */
    @GetMapping("/article/{articleId}")
    public ResponseEntity<SummaryResponseDto> getSummary(
            @PathVariable Long articleId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "BRIEF") Summary.SummaryType summaryType) {

        accessControl.requireSelfOrAdminIfPresent(userId);
        Optional<SummaryResponseDto> summary =
                summaryService.getSummaryAsDto(articleId, userId, summaryType);
        return summary
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get default summary for an article
     * GET /api/v1/summaries/article/{articleId}/default
     */
    @GetMapping("/article/{articleId}/default")
    public ResponseEntity<SummaryResponseDto> getDefaultSummary(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "BRIEF") Summary.SummaryType summaryType) {

        Optional<SummaryResponseDto> summary =
                summaryService.getDefaultSummaryAsDto(articleId, summaryType);
        return summary
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get summary by ID
     * GET /api/v1/summaries/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<SummaryResponseDto> getSummaryById(@PathVariable Long id) {
        SummaryResponseDto summary = summaryService.getSummaryByIdAsDto(id);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get all summaries for an article
     * GET /api/v1/summaries/article/{articleId}/all
     */
    @GetMapping("/article/{articleId}/all")
    public ResponseEntity<List<SummaryResponseDto>> getArticleSummaries(@PathVariable Long articleId) {
        List<SummaryResponseDto> summaries = summaryService.getArticleSummariesAsDtos(articleId);
        return ResponseEntity.ok(summaries);
    }

    /**
     * Get all summaries for a user (paginated)
     * GET /api/v1/summaries/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<SummaryResponseDto>> getUserSummaries(
            @PathVariable Long userId,
            Pageable pageable) {

        accessControl.requireSelfOrAdmin(userId);
        Page<SummaryResponseDto> summaries = summaryService.getUserSummariesAsDtos(userId, pageable);
        return ResponseEntity.ok(summaries);
    }

    /**
     * Regenerate a summary
     * POST /api/v1/summaries/{id}/regenerate
     */
    @PostMapping("/{id}/regenerate")
    public ResponseEntity<SummaryJobResponseDto> regenerateSummary(@PathVariable Long id) {
        SummaryJobResponseDto job = summaryService.regenerateSummaryAsDto(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
    }

    /**
     * Get job status by ID
     * GET /api/v1/summaries/jobs/{jobId}
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<SummaryJobResponseDto> getJobById(@PathVariable Long jobId) {
        SummaryJobResponseDto job = summaryService.getJobByIdAsDto(jobId);
        return ResponseEntity.ok(job);
    }

    /**
     * Get all queued jobs
     * GET /api/v1/summaries/jobs/queued
     */
    @GetMapping("/jobs/queued")
    public ResponseEntity<List<SummaryJobResponseDto>> getQueuedJobs() {
        accessControl.requireWorker();
        List<SummaryJobResponseDto> jobs = summaryService.getQueuedJobsAsDtos();
        return ResponseEntity.ok(jobs);
    }

    /**
     * Retry a failed job
     * POST /api/v1/summaries/jobs/{jobId}/retry
     */
    @PostMapping("/jobs/{jobId}/retry")
    public ResponseEntity<SummaryJobResponseDto> retryFailedJob(@PathVariable Long jobId) {
        SummaryJobResponseDto job = summaryService.retryFailedJobAsDto(jobId);
        return ResponseEntity.ok(job);
    }

    /**
     * Get job statistics
     * GET /api/v1/summaries/jobs/statistics
     */
    @GetMapping("/jobs/statistics")
    public ResponseEntity<SummaryService.JobStatistics> getJobStatistics() {
        accessControl.requireWorker();
        SummaryService.JobStatistics stats = summaryService.getJobStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Check if summary exists
     * GET /api/v1/summaries/exists
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> summaryExists(
            @RequestParam Long articleId,
            @RequestParam(required = false) Long userId,
            @RequestParam Summary.SummaryType summaryType) {

        accessControl.requireSelfOrAdminIfPresent(userId);
        boolean exists = summaryService.summaryExists(articleId, userId, summaryType);
        return ResponseEntity.ok(exists);
    }

    /**
     * Worker callback endpoint (called by Python worker when summary is complete)
     * POST /api/v1/summaries/callback/complete
     */
    @PostMapping("/callback/complete")
    public ResponseEntity<Void> handleWorkerCallback(
            @RequestParam Long jobId,
            @RequestParam String summaryText,
            @RequestParam String modelVersion,
            @RequestParam(required = false) Integer tokenCount) {

        accessControl.requireWorker();
        summaryService.handleWorkerCallback(jobId, summaryText, modelVersion, tokenCount);
        return ResponseEntity.ok().build();
    }

    /**
     * Worker failure callback endpoint
     * POST /api/v1/summaries/callback/failure
     */
    @PostMapping("/callback/failure")
    public ResponseEntity<Void> handleWorkerFailure(
            @RequestParam Long jobId,
            @RequestParam String errorMessage) {

        accessControl.requireWorker();
        summaryService.handleWorkerFailure(jobId, errorMessage);
        return ResponseEntity.ok().build();
    }
}