package com.study.synopsi.controller;

import com.study.synopsi.dto.*;
import com.study.synopsi.service.PersonalizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/personalization")
@RequiredArgsConstructor
@Slf4j
public class PersonalizationController {

    private final PersonalizationService personalizationService;

    /**
     * GET /api/v1/personalization/feed/{userId}
     * Get personalized article feed for a user
     */
    @GetMapping("/feed/{userId}")
    public ResponseEntity<Page<PersonalizedArticleDto>> getPersonalizedFeed(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "relevanceScore", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        
        log.info("GET /api/v1/personalization/feed/{} - page: {}, size: {}", 
                userId, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<PersonalizedArticleDto> feed = personalizationService.getPersonalizedArticles(userId, pageable);
        return ResponseEntity.ok(feed);
    }

    /**
     * POST /api/v1/personalization/interactions/{userId}/read
     * Record a reading interaction
     */
    @PostMapping("/interactions/{userId}/read")
    public ResponseEntity<Void> recordReadingInteraction(
            @PathVariable Long userId,
            @Valid @RequestBody ArticleInteractionDto interaction) {
        
        log.info("POST /api/v1/personalization/interactions/{}/read - article: {}", 
                userId, interaction.getArticleId());
        
        personalizationService.recordReadingInteraction(userId, interaction);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * POST /api/v1/personalization/interactions/{userId}/feedback
     * Record user feedback (like, save, etc.)
     */
    @PostMapping("/interactions/{userId}/feedback")
    public ResponseEntity<Void> recordFeedback(
            @PathVariable Long userId,
            @Valid @RequestBody ArticleInteractionDto interaction) {
        
        log.info("POST /api/v1/personalization/interactions/{}/feedback - article: {}, type: {}", 
                userId, interaction.getArticleId(), interaction.getFeedbackType());
        
        personalizationService.recordFeedback(userId, interaction);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * GET /api/v1/personalization/preferences/{userId}
     * Get user's topic preferences
     */
    @GetMapping("/preferences/{userId}")
    public ResponseEntity<List<UserPreferenceDto>> getUserPreferences(@PathVariable Long userId) {
        log.info("GET /api/v1/personalization/preferences/{}", userId);
        
        List<UserPreferenceDto> preferences = personalizationService.getUserPreferences(userId);
        return ResponseEntity.ok(preferences);
    }

    /**
     * PUT /api/v1/personalization/preferences/{userId}
     * Update or create a user preference
     */
    @PutMapping("/preferences/{userId}")
    public ResponseEntity<UserPreferenceDto> updateUserPreference(
            @PathVariable Long userId,
            @Valid @RequestBody UserPreferenceDto preferenceDto) {
        
        log.info("PUT /api/v1/personalization/preferences/{} - topic: {}", 
                userId, preferenceDto.getTopicId());
        
        UserPreferenceDto updated = personalizationService.updateUserPreference(userId, preferenceDto);
        return ResponseEntity.ok(updated);
    }

    /**
     * GET /api/v1/personalization/interests/{userId}
     * Get user's inferred interests based on behavior
     */
    @GetMapping("/interests/{userId}")
    public ResponseEntity<List<UserTopicInterestDto>> getInferredInterests(@PathVariable Long userId) {
        log.info("GET /api/v1/personalization/interests/{}", userId);
        
        List<UserTopicInterestDto> interests = personalizationService.getInferredInterests(userId);
        return ResponseEntity.ok(interests);
    }

    /**
     * GET /api/v1/personalization/similar/{userId}/{articleId}
     * Get similar articles to a given article
     */
    @GetMapping("/similar/{userId}/{articleId}")
    public ResponseEntity<List<PersonalizedArticleDto>> getSimilarArticles(
            @PathVariable Long userId,
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("GET /api/v1/personalization/similar/{}/{} - limit: {}", 
                userId, articleId, limit);
        
        List<PersonalizedArticleDto> similar = personalizationService
                .getSimilarArticles(userId, articleId, limit);
        return ResponseEntity.ok(similar);
    }

    /**
     * Exception handler for this controller
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Error in PersonalizationController: {}", ex.getMessage(), ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Simple error response DTO
     */
    record ErrorResponse(int status, String message, long timestamp) {}
}