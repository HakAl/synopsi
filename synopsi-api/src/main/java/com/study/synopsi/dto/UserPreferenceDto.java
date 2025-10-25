package com.study.synopsi.dto;

import com.study.synopsi.model.UserPreference;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceDto {
    private Long id;

    @NotNull(message = "Topic ID is required")
    private Long topicId;

    private String topicName;

    // Optional - defaults to MEDIUM if not provided
    @Builder.Default
    private UserPreference.InterestLevel interestLevel = UserPreference.InterestLevel.MEDIUM;

    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}