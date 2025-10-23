package com.study.synopsi.service;

import com.study.synopsi.dto.ArticleRequestDto;
import com.study.synopsi.dto.ArticleResponseDto;
import com.study.synopsi.mapper.ArticleMapper;
import com.study.synopsi.model.Article;
import com.study.synopsi.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;

    /**
     * Get all articles
     */
    @Transactional(readOnly = true)
    public List<ArticleResponseDto> getAllArticles() {
        List<Article> articles = articleRepository.findAll();
        return articleMapper.toDtoList(articles);
    }

    /**
     * Get article by ID
     */
    @Transactional(readOnly = true)
    public ArticleResponseDto getArticleById(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ArticleNotFoundException("Article not found with id: " + id));
        return articleMapper.toDto(article);
    }

    /**
     * Create new article (from Python worker or API)
     */
    @Transactional
    public ArticleResponseDto createArticle(ArticleRequestDto requestDto) {
        // Mapper handles feed validation and conversion
        Article article = articleMapper.toEntity(requestDto);

        // Save to database
        Article savedArticle = articleRepository.save(article);

        // Return as DTO
        return articleMapper.toDto(savedArticle);
    }

    /**
     * Update existing article (partial update)
     */
    @Transactional
    public ArticleResponseDto updateArticle(Long id, ArticleRequestDto requestDto) {
        Article existingArticle = articleRepository.findById(id)
                .orElseThrow(() -> new ArticleNotFoundException("Article not found with id: " + id));

        // Update only mutable fields
        articleMapper.updateEntityFromDto(requestDto, existingArticle);

        // Save and return
        Article updatedArticle = articleRepository.save(existingArticle);
        return articleMapper.toDto(updatedArticle);
    }

    /**
     * Delete article by ID
     */
    @Transactional
    public void deleteArticle(Long id) {
        if (!articleRepository.existsById(id)) {
            throw new ArticleNotFoundException("Article not found with id: " + id);
        }
        articleRepository.deleteById(id);
    }

    // Custom exception
    public static class ArticleNotFoundException extends RuntimeException {
        public ArticleNotFoundException(String message) {
            super(message);
        }
    }
}