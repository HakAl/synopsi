package com.study.synopsi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "feeds", indexes = {
        @Index(name = "idx_feed_last_crawled", columnList = "lastCrawled"),
        @Index(name = "idx_feed_is_active", columnList = "isActive")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Column(nullable = false, unique = true, length = 2048)
    private String feedUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedType feedType = FeedType.RSS;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic; // Primary topic for this feed

    @Column(nullable = false)
    private Integer crawlFrequencyMinutes = 60; // How often to check this feed

    @Column
    private LocalDateTime lastCrawled;

    @Column
    private LocalDateTime lastSuccessfulCrawl;

    @Column
    private LocalDateTime lastFailedCrawl;

    @Column(columnDefinition = "TEXT")
    private String lastError; // Store last error message for debugging

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Integer priority = 5; // 1-10, higher = more important

    @Column(nullable = false)
    private Integer failureCount = 0; // Track consecutive failures

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // One feed has many articles
    @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL)
    private Set<Article> articles = new HashSet<>();

    public enum FeedType {
        RSS,
        ATOM,
        JSON,
        OTHER
    }

    // Helper methods
    public void addArticle(Article article) {
        articles.add(article);
        article.setFeed(this);
    }

    public void recordSuccessfulCrawl() {
        this.lastCrawled = LocalDateTime.now();
        this.lastSuccessfulCrawl = LocalDateTime.now();
        this.failureCount = 0;
        this.lastError = null;
    }

    public void recordFailedCrawl(String errorMessage) {
        this.lastCrawled = LocalDateTime.now();
        this.lastFailedCrawl = LocalDateTime.now();
        this.failureCount++;
        this.lastError = errorMessage;
    }
}