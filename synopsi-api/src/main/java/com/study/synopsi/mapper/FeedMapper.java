package com.study.synopsi.mapper;

import com.study.synopsi.dto.FeedRequestDto;
import com.study.synopsi.dto.FeedResponseDto;
import com.study.synopsi.model.Feed;
import com.study.synopsi.model.Source;
import com.study.synopsi.model.Topic;
//todo
//import com.study.synopsi.repository.SourceRepository;
//import com.study.synopsi.repository.TopicRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class FeedMapper {

//    @Autowired
//    protected SourceRepository sourceRepository;

//    @Autowired
//    protected TopicRepository topicRepository;

    // Entity → DTO (for returning feed in responses)
    @Mapping(target = "sourceId", source = "source.id")
    @Mapping(target = "sourceName", source = "source.name")
    @Mapping(target = "topicId", source = "topic.id")
    @Mapping(target = "topicName", source = "topic.name")
    @Mapping(target = "articleCount", expression = "java(getArticleCount(feed))")
    public abstract FeedResponseDto toDto(Feed feed);

    // DTO → Entity (for creating new feeds)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "source", source = "sourceId", qualifiedByName = "sourceIdToSource")
    @Mapping(target = "topic", source = "topicId", qualifiedByName = "topicIdToTopic")
    @Mapping(target = "articles", ignore = true)
    @Mapping(target = "lastCrawled", ignore = true)
    @Mapping(target = "lastSuccessfulCrawl", ignore = true)
    @Mapping(target = "lastFailedCrawl", ignore = true)
    @Mapping(target = "lastError", ignore = true)
    @Mapping(target = "failureCount", constant = "0")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract Feed toEntity(FeedRequestDto dto);

    // Update existing entity from DTO (for PATCH/PUT operations)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "source", source = "sourceId", qualifiedByName = "sourceIdToSource")
    @Mapping(target = "topic", source = "topicId", qualifiedByName = "topicIdToTopic")
    @Mapping(target = "articles", ignore = true)
    @Mapping(target = "lastCrawled", ignore = true)
    @Mapping(target = "lastSuccessfulCrawl", ignore = true)
    @Mapping(target = "lastFailedCrawl", ignore = true)
    @Mapping(target = "lastError", ignore = true)
    @Mapping(target = "failureCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract void updateEntityFromDto(FeedRequestDto dto, @MappingTarget Feed feed);

    // Custom mapping methods

    @Named("sourceIdToSource")
    protected Source sourceIdToSource(Long sourceId) {
        if (sourceId == null) {
            return null;
        }
        return null;

//        return sourceRepository.findById(sourceId)
//                .orElseThrow(() -> new IllegalArgumentException("Source not found with id: " + sourceId));
    }

    @Named("topicIdToTopic")
    protected Topic topicIdToTopic(Long topicId) {
        if (topicId == null) {
            return null;
        }
        return null;

//        return topicRepository.findById(topicId)
//                .orElseThrow(() -> new IllegalArgumentException("Topic not found with id: " + topicId));
    }

    protected Integer getArticleCount(Feed feed) {
        return feed.getArticles() != null ? feed.getArticles().size() : 0;
    }

    // Helper method for batch conversions
    public abstract List<FeedResponseDto> toDtoList(List<Feed> feeds);
}