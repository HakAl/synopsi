package com.study.synopsi.dto;

import com.study.synopsi.model.Source;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceRequestDto {

    @NotBlank(message = "Source name is required")
    @Size(max = 200, message = "Source name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "Base URL is required")
    @Size(max = 500, message = "Base URL must not exceed 500 characters")
    @Pattern(regexp = "^https?://.*", message = "Base URL must start with http:// or https://")
    private String baseUrl;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @DecimalMin(value = "0.0", message = "Credibility score must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Credibility score must be between 0.0 and 1.0")
    private Double credibilityScore;

    @Size(max = 100, message = "Language must not exceed 100 characters")
    private String language;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    @NotNull(message = "Source type is required")
    private Source.SourceType sourceType;

    private Boolean isActive;
}