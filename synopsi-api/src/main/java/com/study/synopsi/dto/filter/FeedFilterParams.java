package com.study.synopsi.dto.filter;

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
public class FeedFilterParams {

    private Long sourceId;
    private Feed.FeedType feedType;
    private Boolean isActive;
    private Long topicId;
    private Integer minPriority;
    private Integer maxPriority;
    private LocalDateTime lastCrawledAfter;
    private LocalDateTime lastCrawledBefore;
    private Integer minFailureCount;
    private Integer maxFailureCount;
    private String searchTerm; // Search in title, description, or URL
}