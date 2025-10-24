package com.study.synopsi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicRequestDto {

    @NotBlank(message = "Topic name is required")
    @Size(max = 100, message = "Topic name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotBlank(message = "Slug is required")
    @Size(max = 100, message = "Slug must not exceed 100 characters")
    @jakarta.validation.constraints.Pattern(
        regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", 
        message = "Slug must be lowercase, alphanumeric, and hyphen-separated (e.g., 'machine-learning')"
    )
    private String slug;

    private Long parentTopicId; // Optional - for hierarchical topics

    private Boolean isActive;
}