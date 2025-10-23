package com.study.synopsi.dto;

import com.study.synopsi.model.Feed;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedResponseDto {

    private Long id;
    private Long sourceId;
    private String sourceName;
    private String feedUrl;
    private Feed.FeedType feedType;
    private String title;
    private String description;
    private Long topicId;
    private String topicName;
    private Integer crawlFrequencyMinutes;
    private LocalDateTime lastCrawled;
    private LocalDateTime lastSuccessfulCrawl;
    private LocalDateTime lastFailedCrawl;
    private String lastError;
    private Boolean isActive;
    private Integer priority;
    private Integer failureCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer articleCount; // Number of articles from this feed
}