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
@Table(name = "articles", indexes = {
        @Index(name = "idx_article_publication_date", columnList = "publicationDate"),
        @Index(name = "idx_article_feed_id", columnList = "feed_id"),
        @Index(name = "idx_article_created_at", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, unique = true, length = 2048)
    private String originalUrl;

    @Column(columnDefinition = "TEXT")
    private String content; // Full article text

    @Column(columnDefinition = "TEXT")
    private String summary; // AI-generated summary

    @Column(nullable = false)
    private LocalDateTime publicationDate;

    @Column(length = 200)
    private String author;

    @Column(length = 2048)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description; // Meta description or excerpt

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArticleStatus status = ArticleStatus.PENDING;

    @Column
    private Integer readTimeMinutes; // Estimated reading time

    @Column(length = 100)
    private String language; // e.g., "en", "es", "fr"

    // Timestamps for our system
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime summarizedAt; // When summary was generated

    // Foreign key relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    // Many-to-many with Topic through ArticleTopic
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ArticleTopic> articleTopics = new HashSet<>();

    // Relationships with user interactions
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ReadingHistory> readingHistory = new HashSet<>();

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserArticleFeedback> articleFeedback = new HashSet<>();

    // Enum for article processing status
    public enum ArticleStatus {
        PENDING,        // Just fetched, not yet processed
        PROCESSING,     // Currently being summarized
        SUMMARIZED,     // Summary generated successfully
        FAILED,         // Summary generation failed
        ARCHIVED        // Old article, archived
    }

    // Helper methods for managing relationships
    public void addArticleTopic(ArticleTopic articleTopic) {
        articleTopics.add(articleTopic);
        articleTopic.setArticle(this);
    }

    public void removeArticleTopic(ArticleTopic articleTopic) {
        articleTopics.remove(articleTopic);
        articleTopic.setArticle(null);
    }

    public void addReadingHistory(ReadingHistory history) {
        readingHistory.add(history);
        history.setArticle(this);
    }

    public void addArticleFeedback(UserArticleFeedback feedback) {
        articleFeedback.add(feedback);
        feedback.setArticle(this);
    }

    // Convenience method to get the source through feed
    public Source getSource() {
        return feed != null ? feed.getSource() : null;
    }
}