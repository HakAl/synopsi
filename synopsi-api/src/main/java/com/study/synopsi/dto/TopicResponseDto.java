package com.study.synopsi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicResponseDto {

    private Long id;
    private String name;
    private String description;
    private String slug;
    private Long parentTopicId;
    private String parentTopicName;
    private List<String> hierarchyPath; // Full path from root to this topic
    private Integer depth; // 0 for root topics, increases for nested topics
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer childTopicCount; // Number of direct child topics
}