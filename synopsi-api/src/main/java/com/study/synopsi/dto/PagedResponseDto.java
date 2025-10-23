package com.study.synopsi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic wrapper for paginated responses
 * Provides page metadata alongside the actual content
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponseDto<T> {

    /**
     * The actual content/data for this page
     */
    private List<T> content;

    /**
     * Current page number (0-indexed)
     */
    private int pageNumber;

    /**
     * Number of items per page
     */
    private int pageSize;

    /**
     * Total number of items across all pages
     */
    private long totalElements;

    /**
     * Total number of pages
     */
    private int totalPages;

    /**
     * Whether this is the last page
     */
    private boolean last;

    /**
     * Whether this is the first page
     */
    private boolean first;

    /**
     * Whether the page is empty
     */
    private boolean empty;

    /**
     * Number of elements in current page
     */
    private int numberOfElements;
}