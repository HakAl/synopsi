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
public class SummaryResponseDto {
    private Long id;
    private Long articleId;
    private String articleTitle; // Include basic article info for convenience
    private String summaryText;
    private String summaryType;
    private String summaryLength;
    private String modelVersion;
    private LocalDateTime generatedAt;
    private Integer tokenCount;
    private String status;
    private Integer regenerationCount;
    private LocalDateTime createdAt;
    private Boolean isPersonalized; // true if user-specific, false if default
}