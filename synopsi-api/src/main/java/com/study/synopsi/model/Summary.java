package com.study.synopsi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "summaries", indexes = {
        @Index(name = "idx_summary_article_user", columnList = "article_id, user_id"),
        @Index(name = "idx_summary_status", columnList = "status"),
        @Index(name = "idx_summary_generated_at", columnList = "generatedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Summary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user; // null = default summary for all users

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summaryText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SummaryType summaryType = SummaryType.BRIEF;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SummaryLength summaryLength = SummaryLength.MEDIUM;

    @Column(length = 100)
    private String modelVersion; // e.g., "pytorch-v1.0"

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @Column
    private Integer tokenCount; // for analytics/cost tracking

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SummaryStatus status = SummaryStatus.COMPLETED;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private Integer regenerationCount = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Enums
    public enum SummaryType {
        BRIEF,      // Short overview
        DETAILED,   // Comprehensive summary
        ELI5,       // Explain like I'm 5
        LIST,      // Summary of each paragraph
        CUSTOM      // User-defined parameters
    }

    public enum SummaryLength {
        SHORT,      // ~50-100 words
        MEDIUM,     // ~100-200 words
        LONG        // ~200-400 words
    }

    public enum SummaryStatus {
        GENERATING, // Currently being generated
        COMPLETED,  // Successfully generated
        FAILED      // Generation failed
    }

    // Helper method to check if this is a default (non-personalized) summary
    public boolean isDefaultSummary() {
        return user == null;
    }

    // Helper method to increment regeneration count
    public void incrementRegenerationCount() {
        this.regenerationCount++;
    }
}