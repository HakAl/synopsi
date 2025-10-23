package com.study.synopsi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "article_topics",
        uniqueConstraints = @UniqueConstraint(columnNames = {"article_id", "topic_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column
    private Double relevanceScore; // 0.0 to 1.0 - how relevant is this topic to the article

    @Column(nullable = false)
    private Boolean isPrimary = false; // Is this the primary topic for the article?

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructor for easy creation
    public ArticleTopic(Article article, Topic topic, Double relevanceScore, Boolean isPrimary) {
        this.article = article;
        this.topic = topic;
        this.relevanceScore = relevanceScore;
        this.isPrimary = isPrimary;
    }
}