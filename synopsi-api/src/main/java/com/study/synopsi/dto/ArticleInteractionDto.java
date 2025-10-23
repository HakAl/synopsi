package com.study.synopsi.dto;

import com.study.synopsi.model.UserArticleFeedback;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleInteractionDto {
    @NotNull
    private Long articleId;
    
    // Reading interaction
    private Integer timeSpentSeconds;
    
    @Min(0)
    @Max(100)
    private Integer completionPercentage;
    
    // Feedback interaction
    private UserArticleFeedback.FeedbackType feedbackType;
    
    @Min(1)
    @Max(5)
    private Integer rating;
    
    private String comment;
}