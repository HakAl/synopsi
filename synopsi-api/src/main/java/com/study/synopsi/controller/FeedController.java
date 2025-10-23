package com.study.synopsi.controller;

import com.study.synopsi.dto.FeedRequestDto;
import com.study.synopsi.dto.FeedResponseDto;
import com.study.synopsi.dto.PagedResponseDto;
import com.study.synopsi.dto.filter.FeedFilterParams;
import com.study.synopsi.model.Feed;
import com.study.synopsi.service.FeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
@Slf4j
public class FeedController {

    private final FeedService feedService;

    // ========================================================================
    // CORE CRUD OPERATIONS
    // ========================================================================

    /**
     * Get filtered and paginated feeds
     * GET /api/feeds?sourceId=1&isActive=true&page=0&size=20&sort=priority,desc
     */
    @GetMapping
    public ResponseEntity<PagedResponseDto<FeedResponseDto>> getFeeds(
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) Feed.FeedType feedType,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) Integer minPriority,
            @RequestParam(required = false) Integer maxPriority,
            @RequestParam(required = false) LocalDateTime lastCrawledAfter,
            @RequestParam(required = false) LocalDateTime lastCrawledBefore,
            @RequestParam(required = false) Integer minFailureCount,
            @RequestParam(required = false) Integer maxFailureCount,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {

        log.info("GET /api/feeds - page: {}, size: {}", page, size);

        // Build filter params
        FeedFilterParams filters = FeedFilterParams.builder()
                .sourceId(sourceId)
                .feedType(feedType)
                .isActive(isActive)
                .topicId(topicId)
                .minPriority(minPriority)
                .maxPriority(maxPriority)
                .lastCrawledAfter(lastCrawledAfter)
                .lastCrawledBefore(lastCrawledBefore)
                .minFailureCount(minFailureCount)
                .maxFailureCount(maxFailureCount)
                .searchTerm(searchTerm)
                .build();

        // Build pageable with sorting
        Sort sortObj = Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        PagedResponseDto<FeedResponseDto> response = feedService.getFilteredFeeds(filters, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get feed by ID
     * GET /api/feeds/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<FeedResponseDto> getFeedById(@PathVariable Long id) {
        log.info("GET /api/feeds/{}", id);
        FeedResponseDto feed = feedService.getFeedById(id);
        return ResponseEntity.ok(feed);
    }

    /**
     * Create new feed
     * POST /api/feeds
     */
    @PostMapping
    public ResponseEntity<FeedResponseDto> createFeed(@Valid @RequestBody FeedRequestDto requestDto) {
        log.info("POST /api/feeds - Creating feed: {}", requestDto.getFeedUrl());
        FeedResponseDto createdFeed = feedService.createFeed(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdFeed);
    }

    /**
     * Update existing feed
     * PUT /api/feeds/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<FeedResponseDto> updateFeed(
            @PathVariable Long id,
            @Valid @RequestBody FeedRequestDto requestDto) {
        log.info("PUT /api/feeds/{}", id);
        FeedResponseDto updatedFeed = feedService.updateFeed(id, requestDto);
        return ResponseEntity.ok(updatedFeed);
    }

    /**
     * Delete feed
     * DELETE /api/feeds/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeed(@PathVariable Long id) {
        log.info("DELETE /api/feeds/{}", id);
        feedService.deleteFeed(id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // QUERY ENDPOINTS
    // ========================================================================

    /**
     * Get all feeds (deprecated - use GET /api/feeds instead)
     * GET /api/feeds/all
     */
    @Deprecated
    @GetMapping("/all")
    public ResponseEntity<List<FeedResponseDto>> getAllFeeds() {
        log.info("GET /api/feeds/all (deprecated)");
        List<FeedResponseDto> feeds = feedService.getAllFeeds();
        return ResponseEntity.ok(feeds);
    }

    /**
     * Get feeds by source ID
     * GET /api/feeds/source/{sourceId}
     */
    @GetMapping("/source/{sourceId}")
    public ResponseEntity<List<FeedResponseDto>> getFeedsBySourceId(@PathVariable Long sourceId) {
        log.info("GET /api/feeds/source/{}", sourceId);
        List<FeedResponseDto> feeds = feedService.getFeedsBySourceId(sourceId);
        return ResponseEntity.ok(feeds);
    }

    /**
     * Get all active feeds
     * GET /api/feeds/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<FeedResponseDto>> getActiveFeeds() {
        log.info("GET /api/feeds/active");
        List<FeedResponseDto> feeds = feedService.getActiveFeeds();
        return ResponseEntity.ok(feeds);
    }

    /**
     * Get feeds that need crawling (critical for worker)
     * GET /api/feeds/needs-crawl
     */
    @GetMapping("/needs-crawl")
    public ResponseEntity<List<FeedResponseDto>> getFeedsNeedingCrawl() {
        log.info("GET /api/feeds/needs-crawl");
        List<FeedResponseDto> feeds = feedService.getFeedsNeedingCrawl();
        log.info("Found {} feeds needing crawl", feeds.size());
        return ResponseEntity.ok(feeds);
    }

    /**
     * Get feeds by priority range
     * GET /api/feeds/priority?min=7&max=10
     */
    @GetMapping("/priority")
    public ResponseEntity<List<FeedResponseDto>> getFeedsByPriorityRange(
            @RequestParam(defaultValue = "1") Integer min,
            @RequestParam(defaultValue = "10") Integer max) {
        log.info("GET /api/feeds/priority?min={}&max={}", min, max);
        List<FeedResponseDto> feeds = feedService.getFeedsByPriorityRange(min, max);
        return ResponseEntity.ok(feeds);
    }

    /**
     * Get feeds by type
     * GET /api/feeds/type/{feedType}
     */
    @GetMapping("/type/{feedType}")
    public ResponseEntity<List<FeedResponseDto>> getFeedsByType(@PathVariable Feed.FeedType feedType) {
        log.info("GET /api/feeds/type/{}", feedType);
        List<FeedResponseDto> feeds = feedService.getFeedsByType(feedType);
        return ResponseEntity.ok(feeds);
    }

    // ========================================================================
    // STATUS MANAGEMENT
    // ========================================================================

    /**
     * Activate a feed
     * PATCH /api/feeds/{id}/activate
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<FeedResponseDto> activateFeed(@PathVariable Long id) {
        log.info("PATCH /api/feeds/{}/activate", id);
        FeedResponseDto feed = feedService.activateFeed(id);
        return ResponseEntity.ok(feed);
    }

    /**
     * Deactivate a feed
     * PATCH /api/feeds/{id}/deactivate
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<FeedResponseDto> deactivateFeed(@PathVariable Long id) {
        log.info("PATCH /api/feeds/{}/deactivate", id);
        FeedResponseDto feed = feedService.deactivateFeed(id);
        return ResponseEntity.ok(feed);
    }

    /**
     * Bulk activate feeds
     * POST /api/feeds/bulk/activate
     * Body: [1, 2, 3, 4, 5]
     */
    @PostMapping("/bulk/activate")
    public ResponseEntity<Void> bulkActivateFeeds(@RequestBody List<Long> feedIds) {
        log.info("POST /api/feeds/bulk/activate - {} feeds", feedIds.size());
        feedService.bulkActivateFeeds(feedIds);
        return ResponseEntity.ok().build();
    }

    /**
     * Bulk deactivate feeds
     * POST /api/feeds/bulk/deactivate
     * Body: [1, 2, 3, 4, 5]
     */
    @PostMapping("/bulk/deactivate")
    public ResponseEntity<Void> bulkDeactivateFeeds(@RequestBody List<Long> feedIds) {
        log.info("POST /api/feeds/bulk/deactivate - {} feeds", feedIds.size());
        feedService.bulkDeactivateFeeds(feedIds);
        return ResponseEntity.ok().build();
    }

    // ========================================================================
    // WORKER INTEGRATION ENDPOINTS
    // ========================================================================

    /**
     * Record successful crawl
     * POST /api/feeds/{id}/crawl/success
     */
    @PostMapping("/{id}/crawl/success")
    public ResponseEntity<Void> recordSuccessfulCrawl(@PathVariable Long id) {
        log.info("POST /api/feeds/{}/crawl/success", id);
        feedService.recordSuccessfulCrawl(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Record failed crawl
     * POST /api/feeds/{id}/crawl/failure
     * Body: { "errorMessage": "Connection timeout" }
     */
    @PostMapping("/{id}/crawl/failure")
    public ResponseEntity<Void> recordFailedCrawl(
            @PathVariable Long id,
            @RequestBody CrawlFailureRequest request) {
        log.warn("POST /api/feeds/{}/crawl/failure - Error: {}", id, request.getErrorMessage());
        feedService.recordFailedCrawl(id, request.getErrorMessage());
        return ResponseEntity.ok().build();
    }

    // ========================================================================
    // HELPER CLASSES
    // ========================================================================

    /**
     * Request DTO for crawl failure endpoint
     */
    public record CrawlFailureRequest(String errorMessage) {
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}