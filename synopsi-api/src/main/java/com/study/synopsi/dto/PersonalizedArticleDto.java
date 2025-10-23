package com.study.synopsi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalizedArticleDto {
    // Core article fields
    private Long articleId;
    private String title;
    private String originalUrl;
    private String summary;
    private LocalDateTime publicationDate;
    private String author;
    private String imageUrl;
    private String description;
    private Integer readTimeMinutes;
    private String language;
    
    // Source/Feed info
    private String feedTitle;
    private String sourceName;
    
    // Topic information
    private List<String> topicNames;
    private List<Long> topicIds;
    
    // Personalization data
    private Double relevanceScore; // 0.0 - 1.0
    private String recommendationReason; // Human-readable explanation
    
    // User interaction data
    private Boolean hasRead;
    private Boolean isLiked;
    private Boolean isSaved;
    private Boolean isDisliked;
    private Integer userRating; // 1-5 if rated
    private Integer readCount; // How many times user read this
    private Integer completionPercentage; // Last read completion %
    private LocalDateTime lastReadAt;
    
    // Engagement metrics (optional - for display)
    private Long totalLikes;
    private Long totalSaves;
    private Double averageRating;
}