package com.study.synopsi.service;

import com.study.synopsi.dto.ArticleRequestDto;
import com.study.synopsi.dto.ArticleResponseDto;
import com.study.synopsi.model.Article;
import com.study.synopsi.model.Feed;
import com.study.synopsi.repository.ArticleRepository;
import com.study.synopsi.repository.FeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final FeedRepository feedRepository;

    /**
     * Get all articles
     */
    @Transactional(readOnly = true)
    public List<ArticleResponseDto> getAllArticles() {
        return articleRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Get article by ID
     */
    @Transactional(readOnly = true)
    public ArticleResponseDto getArticleById(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ArticleNotFoundException("Article not found with id: " + id));
        return toResponseDto(article);
    }

    /**
     * Create new article (from Python worker)
     */
    @Transactional
    public ArticleResponseDto createArticle(ArticleRequestDto requestDto) {
        // Validate feed exists
        Feed feed = feedRepository.findById(requestDto.getFeedId())
                .orElseThrow(() -> new IllegalArgumentException("Feed not found with id: " + requestDto.getFeedId()));

        // Convert DTO to Entity
        Article article = toEntity(requestDto, feed);

        // Save to database
        Article savedArticle = articleRepository.save(article);

        // Return as DTO
        return toResponseDto(savedArticle);
    }

    // ========== Mapper Methods ==========

    private ArticleResponseDto toResponseDto(Article article) {
        ArticleResponseDto dto = new ArticleResponseDto();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setOriginalUrl(article.getOriginalUrl());
        dto.setContent(article.getContent());
        dto.setSummary(article.getSummary());
        dto.setPublicationDate(article.getPublicationDate());
        dto.setAuthor(article.getAuthor());
        dto.setImageUrl(article.getImageUrl());
        dto.setDescription(article.getDescription());
        dto.setStatus(article.getStatus().name());
        dto.setReadTimeMinutes(article.getReadTimeMinutes());
        dto.setLanguage(article.getLanguage());
        dto.setCreatedAt(article.getCreatedAt());
        dto.setSummarizedAt(article.getSummarizedAt());

        // Add feed/source info
        if (article.getFeed() != null) {
            dto.setFeedTitle(article.getFeed().getTitle());
            if (article.getFeed().getSource() != null) {
                dto.setSourceName(article.getFeed().getSource().getName());
            }
        }

        return dto;
    }

    private Article toEntity(ArticleRequestDto dto, Feed feed) {
        Article article = new Article();
        article.setTitle(dto.getTitle());
        article.setOriginalUrl(dto.getOriginalUrl());
        article.setContent(dto.getContent());
        article.setSummary(dto.getSummary());
        article.setPublicationDate(dto.getPublicationDate());
        article.setAuthor(dto.getAuthor());
        article.setImageUrl(dto.getImageUrl());
        article.setDescription(dto.getDescription());
        article.setReadTimeMinutes(dto.getReadTimeMinutes());
        article.setLanguage(dto.getLanguage());
        article.setFeed(feed);

        // Set status based on whether summary exists
        if (dto.getSummary() != null && !dto.getSummary().isEmpty()) {
            article.setStatus(Article.ArticleStatus.SUMMARIZED);
            article.setSummarizedAt(LocalDateTime.now());
        } else {
            article.setStatus(Article.ArticleStatus.PENDING);
        }

        return article;
    }

    // Custom exception
    public static class ArticleNotFoundException extends RuntimeException {
        public ArticleNotFoundException(String message) {
            super(message);
        }
    }
}
