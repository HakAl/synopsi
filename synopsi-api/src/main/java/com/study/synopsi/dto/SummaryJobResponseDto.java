package com.study.synopsi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryJobResponseDto {
    private Long id;
    private Long articleId;
    private String articleTitle;
    private Long userId; // null for default summaries
    private String summaryType;
    private String summaryLength;
    private String status;
    private Integer priority;
    private Integer attempts;
    private Integer maxAttempts;
    private String errorMessage;
    private LocalDateTime submittedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}