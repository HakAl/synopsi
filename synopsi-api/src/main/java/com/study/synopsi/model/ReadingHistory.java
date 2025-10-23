package com.study.synopsi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reading_history",
        indexes = {
                @Index(name = "idx_reading_history_user", columnList = "user_id"),
                @Index(name = "idx_reading_history_article", columnList = "article_id"),
                @Index(name = "idx_reading_history_read_at", columnList = "readAt")
        },
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "article_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false)
    private LocalDateTime readAt;

    @Column
    private Integer timeSpentSeconds; // How long they spent reading

    @Column
    private Integer completionPercentage; // 0-100, estimated scroll depth

    @Column
    private LocalDateTime lastAccessedAt; // Track re-reads

    @Column(nullable = false)
    private Integer accessCount = 1; // How many times they've accessed this article

    // Helper method to update on re-read
    public void recordReRead(Integer timeSpent, Integer completion) {
        this.lastAccessedAt = LocalDateTime.now();
        this.accessCount++;
        if (timeSpent != null) {
            this.timeSpentSeconds = (this.timeSpentSeconds != null ? this.timeSpentSeconds : 0) + timeSpent;
        }
        if (completion != null && completion > this.completionPercentage) {
            this.completionPercentage = completion;
        }
    }
}