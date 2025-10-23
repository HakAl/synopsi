package com.study.synopsi.mapper;

import com.study.synopsi.dto.FeedRequestDto;
import com.study.synopsi.dto.FeedResponseDto;
import com.study.synopsi.model.Feed;
import com.study.synopsi.model.Source;
import com.study.synopsi.model.Topic;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FeedMapper {

    @Mapping(target = "sourceId", source = "source.id")
    @Mapping(target = "sourceName", source = "source.name")
    @Mapping(target = "topicId", source = "topic.id")
    @Mapping(target = "topicName", source = "topic.name")
    @Mapping(target = "articleCount", expression = "java(getArticleCount(feed))")
    FeedResponseDto toDto(Feed feed);

    List<FeedResponseDto> toDtoList(List<Feed> feeds);

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
    Feed toEntity(FeedRequestDto dto);

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
    void updateEntityFromDto(FeedRequestDto dto, @MappingTarget Feed feed);

    // Custom mappings - these will need to be implemented in a separate @Component
    @Named("sourceIdToSource")
    default Source sourceIdToSource(Long sourceId) {
        if (sourceId == null) return null;
        Source source = new Source();
        source.setId(sourceId);
        return source;
    }

    @Named("topicIdToTopic")
    default Topic topicIdToTopic(Long topicId) {
        if (topicId == null) return null;
        Topic topic = new Topic();
        topic.setId(topicId);
        return topic;
    }

    default Integer getArticleCount(Feed feed) {
        return feed.getArticles() != null ? feed.getArticles().size() : 0;
    }
}