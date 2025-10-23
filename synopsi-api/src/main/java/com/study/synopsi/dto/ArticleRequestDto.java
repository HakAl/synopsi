package com.study.synopsi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleRequestDto {
    @NotBlank
    private String title;

    @NotBlank
    @URL
    private String originalUrl;

    @NotBlank
    private String content;

    private String summary;
    private LocalDateTime publicationDate;
    private String author;

    @URL
    private String imageUrl;

    private String description;
    private Integer readTimeMinutes;
    private String language;

    @NotNull
    private Long feedId; // Which feed this article belongs to
}