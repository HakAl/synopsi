package com.study.synopsi.dto.filter;

import com.study.synopsi.model.Article.ArticleStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * DTO for filtering articles via query parameters
 * All fields are optional - null values are ignored in filtering
 */
@Data
public class ArticleFilterParams {

    /**
     * Filter by article processing status
     * Example: ?status=SUMMARIZED
     */
    private ArticleStatus status;

    /**
     * Filter by feed ID (which feed the article came from)
     * Example: ?feedId=5
     */
    private Long feedId;

    /**
     * Filter by source name (e.g., "BBC", "CNN")
     * Joins through feed.source.name
     * Example: ?source=BBC
     */
    private String source;

    /**
     * Filter articles published on or after this date
     * Example: ?startDate=2025-10-01T00:00:00
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    /**
     * Filter articles published on or before this date
     * Example: ?endDate=2025-10-23T23:59:59
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    /**
     * Filter by language code
     * Example: ?language=en
     */
    private String language;

    /**
     * Search term for full-text search across title, content, and description
     * Case-insensitive search
     * Example: ?searchTerm=climate change
     */
    private String searchTerm;

    /**
     * Validate that startDate is before endDate
     */
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true; // Valid if either is null
        }
        return startDate.isBefore(endDate) || startDate.isEqual(endDate);
    }
}