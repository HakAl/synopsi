package com.study.synopsi.controller;

import com.study.synopsi.dto.ArticleStatsResponse;
import com.study.synopsi.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for statistics operations
 */
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /**
     * GET /api/v1/stats/articles - Fetch article statistics
     *
     * @return Article statistics including total count, count by status, and last updated timestamp
     */
    @GetMapping("/articles")
    public ResponseEntity<ArticleStatsResponse> getArticleStats() {
        ArticleStatsResponse stats = statsService.getArticleStats();
        return ResponseEntity.ok(stats);
    }
}
