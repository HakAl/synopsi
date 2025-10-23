package com.study.synopsi.controller;

import com.study.synopsi.dto.ArticleRequestDto;
import com.study.synopsi.dto.ArticleResponseDto;
import com.study.synopsi.dto.PagedResponseDto;
import com.study.synopsi.dto.filter.ArticleFilterParams;
import com.study.synopsi.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for article operations
 * Supports filtering, sorting, and pagination
 */
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * GET /api/articles - Fetch paginated and filtered articles
     *
     * Query parameters:
     * - Filtering: status, feedId, source, startDate, endDate, language, searchTerm
     * - Pagination: page (0-indexed), size (items per page)
     * - Sorting: sort (e.g., sort=publicationDate,desc or sort=title,asc)
     *
     * Examples:
     * - /api/articles?page=0&size=20&sort=publicationDate,desc
     * - /api/articles?status=SUMMARIZED&source=BBC&page=0&size=10
     * - /api/articles?startDate=2025-10-01T00:00:00&endDate=2025-10-23T23:59:59
     * - /api/articles?searchTerm=climate&sort=publicationDate,desc
     *
     * @param filters Filter parameters (all optional)
     * @param pageable Pagination and sorting parameters
     * @return Paginated response with articles and metadata
     */
    @GetMapping
    public ResponseEntity<PagedResponseDto<ArticleResponseDto>> getAllArticles(
            ArticleFilterParams filters,
            @PageableDefault(
                    size = 20,
                    sort = "publicationDate",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        // Validate date range if both dates provided
        if (filters != null && !filters.isValidDateRange()) {
            throw new IllegalArgumentException("startDate must be before or equal to endDate");
        }

        PagedResponseDto<ArticleResponseDto> articles =
                articleService.getFilteredArticles(filters, pageable);

        return ResponseEntity.ok(articles);
    }

    /**
     * GET /api/articles/{id} - Fetch a specific article
     *
     * @param id Article ID
     * @return Article details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponseDto> getArticleById(@PathVariable Long id) {
        ArticleResponseDto article = articleService.getArticleById(id);
        return ResponseEntity.ok(article);
    }

    /**
     * POST /api/articles - Create new article (for Python worker)
     *
     * @param requestDto Article data
     * @return Created article with HTTP 201
     */
    @PostMapping
    public ResponseEntity<ArticleResponseDto> createArticle(
            @Valid @RequestBody ArticleRequestDto requestDto) {
        ArticleResponseDto createdArticle = articleService.createArticle(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdArticle);
    }
}