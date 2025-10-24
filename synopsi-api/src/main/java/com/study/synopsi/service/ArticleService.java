package com.study.synopsi.service;

import com.study.synopsi.dto.ArticleRequestDto;
import com.study.synopsi.dto.ArticleResponseDto;
import com.study.synopsi.dto.PagedResponseDto;
import com.study.synopsi.dto.filter.ArticleFilterParams;
import com.study.synopsi.exception.ArticleNotFoundException;
import com.study.synopsi.mapper.ArticleMapper;
import com.study.synopsi.model.Article;
import com.study.synopsi.repository.ArticleRepository;
import com.study.synopsi.specification.ArticleSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;

    /**
     * Get filtered and paginated articles
     * Supports filtering by status, feed, source, date range, language, and search term
     * Supports sorting and pagination
     */
    @Transactional(readOnly = true)
    public PagedResponseDto<ArticleResponseDto> getFilteredArticles(
            ArticleFilterParams filters,
            Pageable pageable) {

        // Build dynamic query specification from filters
        Specification<Article> specification = ArticleSpecification.buildSpecification(filters);

        // Execute query with pagination
        Page<Article> articlePage = articleRepository.findAll(specification, pageable);

        // Map to DTOs
        List<ArticleResponseDto> dtos = articleMapper.toDtoList(articlePage.getContent());

        // Build paginated response
        return buildPagedResponse(articlePage, dtos);
    }

    /**
     * Get all articles (for backward compatibility - delegates to filtered method)
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
                .orElseThrow(() -> new ArticleNotFoundException(id));
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
                .orElseThrow(() -> new ArticleNotFoundException(id));

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
            throw new ArticleNotFoundException(id);
        }
        articleRepository.deleteById(id);
    }

    /**
     * Helper method to build PagedResponseDto from Spring's Page object
     */
    private <T> PagedResponseDto<T> buildPagedResponse(Page<?> page, List<T> content) {
        PagedResponseDto<T> response = new PagedResponseDto<>();
        response.setContent(content);
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setLast(page.isLast());
        response.setFirst(page.isFirst());
        response.setEmpty(page.isEmpty());
        response.setNumberOfElements(page.getNumberOfElements());
        return response;
    }
}