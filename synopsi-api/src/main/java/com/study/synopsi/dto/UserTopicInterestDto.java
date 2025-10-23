package com.study.synopsi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTopicInterestDto {
    private Long topicId;
    private String topicName;
    
    // Explicit preference
    private String explicitInterestLevel; // From UserPreference
    
    // Implicit signals (inferred from behavior)
    private Long articlesRead;
    private Long articlesLiked;
    private Long articlesSaved;
    private Double averageCompletionRate;
    private Long totalTimeSpentSeconds;
    
    // Combined score
    private Double inferredInterestScore; // 0.0 - 1.0
}