package com.study.synopsi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_article_feedback",
        indexes = {
                @Index(name = "idx_feedback_user", columnList = "user_id"),
                @Index(name = "idx_feedback_article", columnList = "article_id"),
                @Index(name = "idx_feedback_type", columnList = "feedbackType")
        },
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "article_id", "feedbackType"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserArticleFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackType feedbackType;

    @Column
    private Integer rating; // Optional 1-5 star rating

    @Column(columnDefinition = "TEXT")
    private String comment; // Optional user comment

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum FeedbackType {
        LIKED,
        DISLIKED,
        SAVED,
        SHARED,
        REPORTED,
        ARCHIVED
    }
}