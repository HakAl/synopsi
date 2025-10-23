package com.study.synopsi.dto;

import com.study.synopsi.model.Feed;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedRequestDto {

    @NotNull(message = "Source ID is required")
    private Long sourceId;

    @NotBlank(message = "Feed URL is required")
    @Size(max = 2048, message = "Feed URL cannot exceed 2048 characters")
    @Pattern(regexp = "^https?://.*", message = "Feed URL must start with http:// or https://")
    private String feedUrl;

    @NotNull(message = "Feed type is required")
    private Feed.FeedType feedType;

    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    private Long topicId; // Optional primary topic

    @NotNull(message = "Crawl frequency is required")
    @Min(value = 1, message = "Crawl frequency must be at least 1 minute")
    @Max(value = 10080, message = "Crawl frequency cannot exceed 1 week (10080 minutes)")
    private Integer crawlFrequencyMinutes;

    @NotNull(message = "Active status is required")
    private Boolean isActive;

    @NotNull(message = "Priority is required")
    @Min(value = 1, message = "Priority must be between 1 and 10")
    @Max(value = 10, message = "Priority must be between 1 and 10")
    private Integer priority;
}