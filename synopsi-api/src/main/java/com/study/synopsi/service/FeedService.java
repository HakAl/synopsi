package com.study.synopsi.service;

import com.study.synopsi.dto.FeedRequestDto;
import com.study.synopsi.dto.FeedResponseDto;
import com.study.synopsi.dto.PagedResponseDto;
import com.study.synopsi.dto.filter.FeedFilterParams;
import com.study.synopsi.exception.FeedNotFoundException;
import com.study.synopsi.exception.InvalidFeedException;
import com.study.synopsi.mapper.FeedMapper;
import com.study.synopsi.model.Feed;
import com.study.synopsi.repository.FeedRepository;
import com.study.synopsi.specification.FeedSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private final FeedRepository feedRepository;
    private final FeedMapper feedMapper;
    // TODO: Inject SourceService when available for source validation
    // private final SourceService sourceService;

    /**
     * Get filtered and paginated feeds
     * Supports filtering by source, feed type, active status, topic, and priority range
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "feeds", key = "#filters.toString() + '_' + #pageable.toString()", unless = "#result.empty")
    public PagedResponseDto<FeedResponseDto> getFilteredFeeds(
            FeedFilterParams filters,
            Pageable pageable) {
        
        log.debug("Getting filtered feeds with filters: {} and pageable: {}", filters, pageable);
        
        // Build dynamic query specification from filters
        Specification<Feed> specification = FeedSpecification.buildSpecification(filters);
        
        // Execute query with pagination
        Page<Feed> feedPage = feedRepository.findAll(specification, pageable);
        
        // Map to DTOs
        List<FeedResponseDto> dtos = feedMapper.toDtoList(feedPage.getContent());
        
        // Build paginated response
        return buildPagedResponse(feedPage, dtos);
    }

    /**
     * Get all feeds (for backward compatibility)
     * @deprecated Use getFilteredFeeds with null filters instead
     */
    @Deprecated
    @Transactional(readOnly = true)
    public List<FeedResponseDto> getAllFeeds() {
        log.debug("Getting all feeds");
        List<Feed> feeds = feedRepository.findAll();
        return feedMapper.toDtoList(feeds);
    }

    /**
     * Get feed by ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "feeds", key = "#id")
    public FeedResponseDto getFeedById(Long id) {
        log.debug("Getting feed by id: {}", id);
        Feed feed = feedRepository.findById(id)
                .orElseThrow(() -> new FeedNotFoundException(id));
        return feedMapper.toDto(feed);
    }

    /**
     * Get feeds by source ID
     */
    @Transactional(readOnly = true)
    public List<FeedResponseDto> getFeedsBySourceId(Long sourceId) {
        log.debug("Getting feeds for source id: {}", sourceId);
        List<Feed> feeds = feedRepository.findBySourceId(sourceId);
        return feedMapper.toDtoList(feeds);
    }

    /**
     * Get all active feeds
     */
    @Transactional(readOnly = true)
    public List<FeedResponseDto> getActiveFeeds() {
        log.debug("Getting all active feeds");
        List<Feed> feeds = feedRepository.findByIsActiveTrue();
        return feedMapper.toDtoList(feeds);
    }

    /**
     * Get feeds that need to be crawled
     * Returns feeds where lastCrawled + crawlFrequencyMinutes <= now
     */
    @Transactional(readOnly = true)
    public List<FeedResponseDto> getFeedsNeedingCrawl() {
        log.debug("Getting feeds needing crawl");
        List<Feed> feeds = feedRepository.findFeedsNeedingCrawl(LocalDateTime.now());
        return feedMapper.toDtoList(feeds);
    }

    /**
     * Get feeds by priority range
     */
    @Transactional(readOnly = true)
    public List<FeedResponseDto> getFeedsByPriorityRange(Integer minPriority, Integer maxPriority) {
        log.debug("Getting feeds with priority between {} and {}", minPriority, maxPriority);
        
        if (minPriority < 1 || maxPriority > 10 || minPriority > maxPriority) {
            throw new InvalidFeedException("Invalid priority range. Min and max must be between 1-10, and min <= max");
        }
        
        List<Feed> feeds = feedRepository.findByPriorityBetween(minPriority, maxPriority);
        return feedMapper.toDtoList(feeds);
    }

    /**
     * Get feeds by feed type
     */
    @Transactional(readOnly = true)
    public List<FeedResponseDto> getFeedsByType(Feed.FeedType feedType) {
        log.debug("Getting feeds of type: {}", feedType);
        List<Feed> feeds = feedRepository.findByFeedType(feedType);
        return feedMapper.toDtoList(feeds);
    }

    /**
     * Create new feed
     */
    @Transactional
    @CacheEvict(value = "feeds", allEntries = true)
    public FeedResponseDto createFeed(FeedRequestDto requestDto) {
        log.info("Creating new feed: {}", requestDto.getFeedUrl());
        
        // Validate feed data
        validateFeedRequest(requestDto);
        
        // Check for duplicate feed URL
        if (feedRepository.existsByFeedUrl(requestDto.getFeedUrl())) {
            throw new InvalidFeedException("Feed with URL already exists: " + requestDto.getFeedUrl());
        }
        
        // TODO: Validate source exists when SourceService is available
        // Source source = sourceService.getSourceEntityById(requestDto.getSourceId());
        
        // Map DTO to entity
        Feed feed = feedMapper.toEntity(requestDto);
        
        // Save to database
        Feed savedFeed = feedRepository.save(feed);
        
        log.info("Successfully created feed with id: {}", savedFeed.getId());
        return feedMapper.toDto(savedFeed);
    }

    /**
     * Update existing feed (partial update)
     */
    @Transactional
    @CacheEvict(value = "feeds", allEntries = true)
    public FeedResponseDto updateFeed(Long id, FeedRequestDto requestDto) {
        log.info("Updating feed with id: {}", id);
        
        Feed existingFeed = feedRepository.findById(id)
                .orElseThrow(() -> new FeedNotFoundException(id));
        
        // Validate update request
        validateFeedRequest(requestDto);
        
        // Check for duplicate feed URL (excluding current feed)
        if (!existingFeed.getFeedUrl().equals(requestDto.getFeedUrl()) 
                && feedRepository.existsByFeedUrl(requestDto.getFeedUrl())) {
            throw new InvalidFeedException("Feed with URL already exists: " + requestDto.getFeedUrl());
        }
        
        // Update only mutable fields
        feedMapper.updateEntityFromDto(requestDto, existingFeed);
        
        // Save and return
        Feed updatedFeed = feedRepository.save(existingFeed);
        log.info("Successfully updated feed with id: {}", id);
        return feedMapper.toDto(updatedFeed);
    }

    /**
     * Delete feed by ID
     */
    @Transactional
    @CacheEvict(value = "feeds", allEntries = true)
    public void deleteFeed(Long id) {
        log.info("Deleting feed with id: {}", id);
        
        if (!feedRepository.existsById(id)) {
            throw new FeedNotFoundException(id);
        }
        
        feedRepository.deleteById(id);
        log.info("Successfully deleted feed with id: {}", id);
    }

    /**
     * Activate a feed
     */
    @Transactional
    @CacheEvict(value = "feeds", allEntries = true)
    public FeedResponseDto activateFeed(Long id) {
        log.info("Activating feed with id: {}", id);
        
        Feed feed = feedRepository.findById(id)
                .orElseThrow(() -> new FeedNotFoundException(id));
        
        feed.setIsActive(true);
        feed.setFailureCount(0); // Reset failure count on activation
        
        Feed updatedFeed = feedRepository.save(feed);
        log.info("Successfully activated feed with id: {}", id);
        return feedMapper.toDto(updatedFeed);
    }

    /**
     * Deactivate a feed
     */
    @Transactional
    @CacheEvict(value = "feeds", allEntries = true)
    public FeedResponseDto deactivateFeed(Long id) {
        log.info("Deactivating feed with id: {}", id);
        
        Feed feed = feedRepository.findById(id)
                .orElseThrow(() -> new FeedNotFoundException(id));
        
        feed.setIsActive(false);
        
        Feed updatedFeed = feedRepository.save(feed);
        log.info("Successfully deactivated feed with id: {}", id);
        return feedMapper.toDto(updatedFeed);
    }

    /**
     * Record successful crawl for a feed
     * Called by worker after successful crawl
     */
    @Transactional
    @CacheEvict(value = "feeds", key = "#id")
    public void recordSuccessfulCrawl(Long id) {
        log.debug("Recording successful crawl for feed id: {}", id);
        
        Feed feed = feedRepository.findById(id)
                .orElseThrow(() -> new FeedNotFoundException(id));
        
        feed.recordSuccessfulCrawl();
        feedRepository.save(feed);
        
        log.debug("Recorded successful crawl for feed id: {}", id);
    }

    /**
     * Record failed crawl for a feed
     * Called by worker after failed crawl
     */
    @Transactional
    @CacheEvict(value = "feeds", key = "#id")
    public void recordFailedCrawl(Long id, String errorMessage) {
        log.warn("Recording failed crawl for feed id: {} with error: {}", id, errorMessage);
        
        Feed feed = feedRepository.findById(id)
                .orElseThrow(() -> new FeedNotFoundException(id));
        
        feed.recordFailedCrawl(errorMessage);
        feedRepository.save(feed);
        
        log.debug("Recorded failed crawl for feed id: {}. Failure count: {}", id, feed.getFailureCount());
    }

    /**
     * Bulk activate feeds
     */
    @Transactional
    @CacheEvict(value = "feeds", allEntries = true)
    public void bulkActivateFeeds(List<Long> feedIds) {
        log.info("Bulk activating {} feeds", feedIds.size());
        
        List<Feed> feeds = feedRepository.findAllById(feedIds);
        feeds.forEach(feed -> {
            feed.setIsActive(true);
            feed.setFailureCount(0);
        });
        
        feedRepository.saveAll(feeds);
        log.info("Successfully activated {} feeds", feeds.size());
    }

    /**
     * Bulk deactivate feeds
     */
    @Transactional
    @CacheEvict(value = "feeds", allEntries = true)
    public void bulkDeactivateFeeds(List<Long> feedIds) {
        log.info("Bulk deactivating {} feeds", feedIds.size());
        
        List<Feed> feeds = feedRepository.findAllById(feedIds);
        feeds.forEach(feed -> feed.setIsActive(false));
        
        feedRepository.saveAll(feeds);
        log.info("Successfully deactivated {} feeds", feeds.size());
    }

    /**
     * Validate feed request data
     */
    private void validateFeedRequest(FeedRequestDto requestDto) {
        // Validate feed URL
        if (requestDto.getFeedUrl() == null || requestDto.getFeedUrl().trim().isEmpty()) {
            throw new InvalidFeedException("Feed URL cannot be null or empty");
        }
        
        if (requestDto.getFeedUrl().length() > 2048) {
            throw new InvalidFeedException("Feed URL cannot exceed 2048 characters");
        }
        
        // Validate URL format
        if (!isValidUrl(requestDto.getFeedUrl())) {
            throw new InvalidFeedException("Invalid feed URL format: " + requestDto.getFeedUrl());
        }
        
        // Validate priority (1-10)
        if (requestDto.getPriority() != null 
                && (requestDto.getPriority() < 1 || requestDto.getPriority() > 10)) {
            throw new InvalidFeedException("Priority must be between 1 and 10");
        }
        
        // Validate crawl frequency
        if (requestDto.getCrawlFrequencyMinutes() != null && requestDto.getCrawlFrequencyMinutes() < 1) {
            throw new InvalidFeedException("Crawl frequency must be at least 1 minute");
        }
        
        // Validate source ID
        if (requestDto.getSourceId() == null) {
            throw new InvalidFeedException("Source ID cannot be null");
        }
    }

    /**
     * Simple URL validation
     */
    private boolean isValidUrl(String url) {
        try {
            new java.net.URL(url);
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (Exception e) {
            return false;
        }
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