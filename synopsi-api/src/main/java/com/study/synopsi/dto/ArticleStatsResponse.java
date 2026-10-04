package com.study.synopsi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleStatsResponse {
    private Long totalCount;
    private Map<String, Long> countByStatus;
    private LocalDateTime lastUpdated;
}
