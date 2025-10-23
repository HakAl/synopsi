package com.study.synopsi.mapper;

import com.study.synopsi.dto.ArticleRequestDto;
import com.study.synopsi.dto.ArticleResponseDto;
import com.study.synopsi.model.Article;
import com.study.synopsi.model.Feed;
import com.study.synopsi.repository.FeedRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class ArticleMapper {

    @Autowired
    protected FeedRepository feedRepository;

    // DTO → Entity (for creating new articles)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "feed", source = "feedId", qualifiedByName = "feedIdToFeed")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "language", source = "language", defaultValue = "en")
    @Mapping(target = "readTimeMinutes", source = ".", qualifiedByName = "calculateReadTime")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "summarizedAt", ignore = true)
    @Mapping(target = "articleTopics", ignore = true)
    @Mapping(target = "readingHistory", ignore = true)
    @Mapping(target = "articleFeedback", ignore = true)
    public abstract Article toEntity(ArticleRequestDto dto);

    // Entity → DTO (for returning article in responses)
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "feedTitle", source = "feed.title")
    @Mapping(target = "sourceName", source = "feed.source.name")
    public abstract ArticleResponseDto toDto(Article entity);

    // Update existing entity from DTO (for PATCH/PUT operations)
    // Only updates mutable fields, ignores immutable ones
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "originalUrl", ignore = true)
    @Mapping(target = "content", ignore = true)
    @Mapping(target = "feed", ignore = true)
    @Mapping(target = "publicationDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "language", source = "language", defaultValue = "en")
    @Mapping(target = "readTimeMinutes", source = ".", qualifiedByName = "calculateReadTime")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "summarizedAt", ignore = true)
    @Mapping(target = "articleTopics", ignore = true)
    @Mapping(target = "readingHistory", ignore = true)
    @Mapping(target = "articleFeedback", ignore = true)
    public abstract void updateEntityFromDto(ArticleRequestDto dto, @MappingTarget Article entity);

    // Custom mapping methods

    @Named("feedIdToFeed")
    protected Feed feedIdToFeed(Long feedId) {
        if (feedId == null) {
            return null;
        }
        return feedRepository.findById(feedId)
                .orElseThrow(() -> new IllegalArgumentException("Feed not found with id: " + feedId));
    }

    @Named("calculateReadTime")
    protected Integer calculateReadTime(ArticleRequestDto dto) {
        // If readTimeMinutes is already provided, use it
        if (dto.getReadTimeMinutes() != null && dto.getReadTimeMinutes() > 0) {
            return dto.getReadTimeMinutes();
        }

        // Otherwise calculate based on content
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            return 1; // Default minimum read time
        }

        // Average reading speed: 200 words per minute
        String content = dto.getContent().trim();
        int wordCount = content.split("\\s+").length;
        int readTime = (int) Math.ceil(wordCount / 200.0);

        return Math.max(1, readTime); // Minimum 1 minute
    }

    @Named("statusToString")
    protected String statusToString(Article.ArticleStatus status) {
        return status != null ? status.name() : null;
    }

    // Helper method for batch conversions
    public abstract java.util.List<ArticleResponseDto> toDtoList(java.util.List<Article> articles);
}