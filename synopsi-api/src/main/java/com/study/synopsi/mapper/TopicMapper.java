package com.study.synopsi.mapper;

import com.study.synopsi.dto.TopicRequestDto;
import com.study.synopsi.dto.TopicResponseDto;
import com.study.synopsi.model.Topic;
import com.study.synopsi.repository.TopicRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class TopicMapper {

    @Autowired
    protected TopicRepository topicRepository;

    // Entity → DTO (for returning topic in responses)
    @Mapping(target = "parentTopicId", source = "parentTopic.id")
    @Mapping(target = "parentTopicName", source = "parentTopic.name")
    @Mapping(target = "hierarchyPath", source = ".", qualifiedByName = "buildHierarchyPath")
    @Mapping(target = "depth", source = ".", qualifiedByName = "calculateDepth")
    @Mapping(target = "childTopicCount", expression = "java(getChildTopicCount(topic))")
    public abstract TopicResponseDto toDto(Topic topic);

    // DTO → Entity (for creating new topics)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentTopic", source = "parentTopicId", qualifiedByName = "parentTopicIdToTopic")
    @Mapping(target = "childTopics", ignore = true)
    @Mapping(target = "articleTopics", ignore = true)
    @Mapping(target = "userPreferences", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract Topic toEntity(TopicRequestDto dto);

    // Update existing entity from DTO (for PATCH/PUT operations)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentTopic", source = "parentTopicId", qualifiedByName = "parentTopicIdToTopic")
    @Mapping(target = "childTopics", ignore = true)
    @Mapping(target = "articleTopics", ignore = true)
    @Mapping(target = "userPreferences", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract void updateEntityFromDto(TopicRequestDto dto, @MappingTarget Topic topic);

    // Custom mapping methods

    @Named("parentTopicIdToTopic")
    protected Topic parentTopicIdToTopic(Long parentTopicId) {
        if (parentTopicId == null) {
            return null;
        }
        return topicRepository.findById(parentTopicId)
                .orElseThrow(() -> new IllegalArgumentException("Parent topic not found with id: " + parentTopicId));
    }

    @Named("buildHierarchyPath")
    protected List<String> buildHierarchyPath(Topic topic) {
        List<String> path = new ArrayList<>();
        buildPathRecursive(topic, path);
        return path;
    }

    private void buildPathRecursive(Topic topic, List<String> path) {
        if (topic == null) {
            return;
        }
        // Build path from root to current topic
        if (topic.getParentTopic() != null) {
            buildPathRecursive(topic.getParentTopic(), path);
        }
        path.add(topic.getName());
    }

    @Named("calculateDepth")
    protected Integer calculateDepth(Topic topic) {
        int depth = 0;
        Topic current = topic;
        while (current.getParentTopic() != null) {
            depth++;
            current = current.getParentTopic();
        }
        return depth;
    }

    protected Integer getChildTopicCount(Topic topic) {
        return topic.getChildTopics() != null ? topic.getChildTopics().size() : 0;
    }

    // Helper method for batch conversions
    public abstract List<TopicResponseDto> toDtoList(List<Topic> topics);
}