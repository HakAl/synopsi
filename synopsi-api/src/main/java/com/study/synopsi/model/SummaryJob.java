package com.study.synopsi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "summary_jobs", indexes = {
        @Index(name = "idx_job_status", columnList = "status"),
        @Index(name = "idx_job_article", columnList = "article_id"),
        @Index(name = "idx_job_submitted_at", columnList = "submittedAt"),
        @Index(name = "idx_job_priority", columnList = "priority")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SummaryJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user; // null = default summary

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Summary.SummaryType summaryType = Summary.SummaryType.BRIEF;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Summary.SummaryLength summaryLength = Summary.SummaryLength.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.QUEUED;

    @Column(nullable = false)
    private Integer priority = 5; // 1-10, higher = more important

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(nullable = false)
    private Integer maxAttempts = 3;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(length = 255)
    private String workerJobId; // ID from Python worker when it exists

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Job status enum
    public enum JobStatus {
        QUEUED,      // Waiting to be processed
        PROCESSING,  // Currently being processed by worker
        COMPLETED,   // Successfully completed
        FAILED,      // Failed after all retries
        CANCELLED    // Manually cancelled
    }

    // Helper methods
    public void incrementAttempts() {
        this.attempts++;
    }

    public boolean canRetry() {
        return this.attempts < this.maxAttempts;
    }

    public void markAsStarted() {
        this.status = JobStatus.PROCESSING;
        this.startedAt = LocalDateTime.now();
    }

    public void markAsCompleted() {
        this.status = JobStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsFailed(String error) {
        this.status = JobStatus.FAILED;
        this.errorMessage = error;
        this.completedAt = LocalDateTime.now();
    }

    public boolean isDefaultSummaryJob() {
        return user == null;
    }
}