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
public class ArticleResponseDto {
    private Long id;
    private String title;
    private String originalUrl;
    private String content;
    private String summary;
    private LocalDateTime publicationDate;
    private String author;
    private String imageUrl;
    private String description;
    private String status;
    private Integer readTimeMinutes;
    private String language;
    private LocalDateTime createdAt;
    private LocalDateTime summarizedAt;

    // Include source/feed info
    private String feedTitle;
    private String sourceName;
}