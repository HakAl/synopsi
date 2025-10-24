package com.study.synopsi.dto;

import com.study.synopsi.model.Source;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceResponseDto {

    private Long id;
    private String name;
    private String baseUrl;
    private String description;
    private Double credibilityScore;
    private String language;
    private String country;
    private String sourceType; // Enum as string
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer feedCount; // Number of feeds associated with this source
}