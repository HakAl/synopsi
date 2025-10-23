package com.study.synopsi.service;

import com.study.synopsi.dto.FeedRequestDto;
import com.study.synopsi.dto.FeedResponseDto;
import com.study.synopsi.dto.PagedResponseDto;
import com.study.synopsi.dto.filter.FeedFilterParams;
import com.study.synopsi.exception.FeedNotFoundException;
import com.study.synopsi.exception.InvalidFeedException;
import com.study.synopsi.mapper.FeedMapper;
import com.study.synopsi.model.Feed;
import com.study.synopsi.model.Source;
import com.study.synopsi.repository.FeedRepository;
import com.study.synopsi.specification.FeedSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedService Tests")
class FeedServiceTest {

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private FeedMapper feedMapper;

    @InjectMocks
    private FeedService feedService;

    private Feed testFeed;
    private FeedRequestDto testFeedRequestDto;
    private FeedResponseDto testFeedResponseDto;
    private Source testSource;

    @BeforeEach
    void setUp() {
        // Setup test source
        testSource = new Source();
        testSource.setId(1L);
        testSource.setName("Test Source");

        // Setup test feed entity
        testFeed = new Feed();
        testFeed.setId(1L);
        testFeed.setSource(testSource);
        testFeed.setFeedUrl("https://example.com/rss");
        testFeed.setFeedType(Feed.FeedType.RSS);
        testFeed.setTitle("Test Feed");
        testFeed.setDescription("Test Description");
        testFeed.setCrawlFrequencyMinutes(60);
        testFeed.setIsActive(true);
        testFeed.setPriority(5);
        testFeed.setFailureCount(0);
        testFeed.setCreatedAt(LocalDateTime.now());
        testFeed.setUpdatedAt(LocalDateTime.now());

        // Setup test request DTO
        testFeedRequestDto = FeedRequestDto.builder()
                .sourceId(1L)
                .feedUrl("https://example.com/rss")
                .feedType(Feed.FeedType.RSS)
                .title("Test Feed")
                .description("Test Description")
                .crawlFrequencyMinutes(60)
                .isActive(true)
                .priority(5)
                .build();

        // Setup test response DTO
        testFeedResponseDto = FeedResponseDto.builder()
                .id(1L)
                .sourceId(1L)
                .sourceName("Test Source")
                .feedUrl("https://example.com/rss")
                .feedType(Feed.FeedType.RSS)
                .title("Test Feed")
                .description("Test Description")
                .crawlFrequencyMinutes(60)
                .isActive(true)
                .priority(5)
                .failureCount(0)
                .build();
    }

    // ========================================================================
    // CRUD OPERATION TESTS
    // ========================================================================

    @Nested
    @DisplayName("Create Feed Tests")
    class CreateFeedTests {

        @Test
        @DisplayName("Should create feed successfully")
        void shouldCreateFeedSuccessfully() {
            // Given
            when(feedRepository.existsByFeedUrl(anyString())).thenReturn(false);
            when(feedMapper.toEntity(any(FeedRequestDto.class))).thenReturn(testFeed);
            when(feedRepository.save(any(Feed.class))).thenReturn(testFeed);
            when(feedMapper.toDto(any(Feed.class))).thenReturn(testFeedResponseDto);

            // When
            FeedResponseDto result = feedService.createFeed(testFeedRequestDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFeedUrl()).isEqualTo("https://example.com/rss");
            verify(feedRepository).existsByFeedUrl("https://example.com/rss");
            verify(feedRepository).save(any(Feed.class));
            verify(feedMapper).toDto(testFeed);
        }

        @Test
        @DisplayName("Should throw exception when feed URL already exists")
        void shouldThrowExceptionWhenFeedUrlExists() {
            // Given
            when(feedRepository.existsByFeedUrl(anyString())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> feedService.createFeed(testFeedRequestDto))
                    .isInstanceOf(InvalidFeedException.class)
                    .hasMessageContaining("Feed with URL already exists");

            verify(feedRepository).existsByFeedUrl("https://example.com/rss");
            verify(feedRepository, never()).save(any(Feed.class));
        }

        @Test
        @DisplayName("Should throw exception when feed URL is null")
        void shouldThrowExceptionWhenFeedUrlIsNull() {
            // Given
            testFeedRequestDto.setFeedUrl(null);

            // When/Then
            assertThatThrownBy(() -> feedService.createFeed(testFeedRequestDto))
                    .isInstanceOf(InvalidFeedException.class)
                    .hasMessageContaining("Feed URL cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception when feed URL is empty")
        void shouldThrowExceptionWhenFeedUrlIsEmpty() {
            // Given
            testFeedRequestDto.setFeedUrl("   ");

            // When/Then
            assertThatThrownBy(() -> feedService.createFeed(testFeedRequestDto))
                    .isInstanceOf(InvalidFeedException.class)
                    .hasMessageContaining("Feed URL cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception when feed URL exceeds max length")
        void shouldThrowExceptionWhenFeedUrlTooLong() {
            // Given
            String longUrl = "https://example.com/" + "x".repeat(2050);
            testFeedRequestDto.setFeedUrl(longUrl);

            // When/Then
            assertThatThrownBy(() -> feedService.createFeed(testFeedRequestDto))
                    .isInstanceOf(InvalidFeedException.class)
                    .hasMessageContaining("Feed URL cannot exceed 2048 characters");
        }

        @Test
        @DisplayName("Should throw exception when feed URL format is invalid")
        void shouldThrowExceptionWhenFeedUrlFormatInvalid() {
            // Given
            testFeedRequestDto.setFeedUrl("not-a-valid-url");

            // When/Then
            assertThatThrownBy(() -> feedService.createFeed(testFeedRequestDto))
                    .isInstanceOf(InvalidFeedException.class)
                    .hasMessageContaining("Invalid feed URL format");
        }

        @Test
        @DisplayName("Should throw exception when priority is less than 1")
        void shouldThrowExceptionWhenPriorityTooLow() {
            // Given
            testFeedRequestDto.setPriority(0);

            // When/Then
            assertThatThrownBy(() -> feedService.createFeed(testFeedRequestDto))
                    .isInstanceOf(InvalidFeedException.class)
                    .hasMessageContaining("Priority must be between 1 and 10");
        }

        @Test
        @DisplayName("Should throw exception when priority is greater than 10")
        void shouldThrowExceptionWhenPriorityTooHigh() {
            // Given
            testFeedRequestDto.setPriority(11);

            // When/Then
            assertThatThrownBy(() -> feedService.createFeed(testFeedRequestDto))
                    .isInstanceOf(InvalidFeedException.class)
                    .hasMessageContaining("Priority must be between 1 and 10");
        }

        @Test
        @DisplayName("Should throw exception when crawl frequency is less than 1")
        void shouldThrowExceptionWhenCrawlFrequencyTooLow() {
            // Given
            testFeedRequestDto.setCrawlFrequencyMinutes(0);

            // When/Then
            assertThatThrownBy(() -> feedService.createFeed(testFeedRequestDto))
                    .isInstanceOf(InvalidFeedException.class)
                    .hasMessageContaining("Crawl frequency must be at least 1 minute");
        }

        @Test
        @DisplayName("Should throw exception when source ID is null")
        void shouldThrowExceptionWhenSourceIdIsNull() {
            // Given
            testFeedRequestDto.setSourceId(null);

            // When/Then
            assertThatThrownBy(() -> feedService.createFeed(testFeedRequestDto))
                    .isInstanceOf(InvalidFeedException.class)
                    .hasMessageContaining("Source ID cannot be null");
        }
    }

    @Nested
    @DisplayName("Get Feed Tests")
    class GetFeedTests {

        @Test
        @DisplayName("Should get feed by ID successfully")
        void shouldGetFeedByIdSuccessfully() {
            // Given
            when(feedRepository.findById(1L)).thenReturn(Optional.of(testFeed));
            when(feedMapper.toDto(any(Feed.class))).thenReturn(testFeedResponseDto);

            // When
            FeedResponseDto result = feedService.getFeedById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(feedRepository).findById(1L);
            verify(feedMapper).toDto(testFeed);
        }

        @Test
        @DisplayName("Should throw exception when feed not found")
        void shouldThrowExceptionWhenFeedNotFound() {
            // Given
            when(feedRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> feedService.getFeedById(999L))
                    .isInstanceOf(FeedNotFoundException.class);

            verify(feedRepository).findById(999L);
        }

        @Test
        @DisplayName("Should get all feeds successfully")
        void shouldGetAllFeedsSuccessfully() {
            // Given
            List<Feed> feeds = Arrays.asList(testFeed);
            List<FeedResponseDto> responseDtos = Arrays.asList(testFeedResponseDto);

            when(feedRepository.findAll()).thenReturn(feeds);
            when(feedMapper.toDtoList(feeds)).thenReturn(responseDtos);

            // When
            List<FeedResponseDto> result = feedService.getAllFeeds();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            verify(feedRepository).findAll();
        }

        @Test
        @DisplayName("Should get feeds by source ID")
        void shouldGetFeedsBySourceId() {
            // Given
            List<Feed> feeds = Arrays.asList(testFeed);
            List<FeedResponseDto> responseDtos = Arrays.asList(testFeedResponseDto);

            when(feedRepository.findBySourceId(1L)).thenReturn(feeds);
            when(feedMapper.toDtoList(feeds)).thenReturn(responseDtos);

            // When
            List<FeedResponseDto> result = feedService.getFeedsBySourceId(1L);

            // Then
            assertThat(result).hasSize(1);
            verify(feedRepository).findBySourceId(1L);
        }

        @Test
        @DisplayName("Should get active feeds")
        void shouldGetActiveFeeds() {
            // Given
            List<Feed> feeds = Arrays.asList(testFeed);
            List<FeedResponseDto> responseDtos = Arrays.asList(testFeedResponseDto);

            when(feedRepository.findByIsActiveTrue()).thenReturn(feeds);
            when(feedMapper.toDtoList(feeds)).thenReturn(responseDtos);

            // When
            List<FeedResponseDto> result = feedService.getActiveFeeds();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIsActive()).isTrue();
            verify(feedRepository).findByIsActiveTrue();
        }

        @Test
        @DisplayName("Should get feeds needing crawl")
        void shouldGetFeedsNeedingCrawl() {
            // Given
            List<Feed> feeds = Arrays.asList(testFeed);
            List<FeedResponseDto> responseDtos = Arrays.asList(testFeedResponseDto);

            when(feedRepository.findFeedsNeedingCrawl(any(LocalDateTime.class))).thenReturn(feeds);
            when(feedMapper.toDtoList(feeds)).thenReturn(responseDtos);

            // When
            List<FeedResponseDto> result = feedService.getFeedsNeedingCrawl();

            // Then
            assertThat(result).hasSize(1);
            verify(feedRepository).findFeedsNeedingCrawl(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should get feeds by type")
        void shouldGetFeedsByType() {
            // Given
            List<Feed> feeds = Arrays.asList(testFeed);
            List<FeedResponseDto> responseDtos = Arrays.asList(testFeedResponseDto);

            when(feedRepository.findByFeedType(Feed.FeedType.RSS)).thenReturn(feeds);
            when(feedMapper.toDtoList(feeds)).thenReturn(responseDtos);

            // When
            List<FeedResponseDto> result = feedService.getFeedsByType(Feed.FeedType.RSS);

            // Then
            assertThat(result).hasSize(1);
            verify(feedRepository).findByFeedType(Feed.FeedType.RSS);
        }

        @Test
        @DisplayName("Should get feeds by priority range")
        void shouldGetFeedsByPriorityRange() {
            // Given
            List<Feed> feeds = Arrays.asList(testFeed);
            List<FeedResponseDto> responseDtos = Arrays.asList(testFeedResponseDto);

            when(feedRepository.findByPriorityBetween(5, 10)).thenReturn(feeds);
            when(feedMapper.toDtoList(feeds)).thenReturn(responseDtos);

            // When
            List<FeedResponseDto> result = feedService.getFeedsByPriorityRange(5, 10);

            // Then
            assertThat(result).hasSize(1);
            verify(feedRepository).findByPriorityBetween(5, 10);
        }

        @Test
        @DisplayName("Should throw exception for invalid priority range")
        void shouldThrowExceptionForInvalidPriorityRange() {
            // When/Then - min > max
            assertThatThrownBy(() -> feedService.getFeedsByPriorityRange(10, 5))
                    .isInstanceOf(InvalidFeedException.class)
                    .hasMessageContaining("Invalid priority range");

            // When/Then - min < 1
            assertThatThrownBy(() -> feedService.getFeedsByPriorityRange(0, 10))
                    .isInstanceOf(InvalidFeedException.class)
                    .hasMessageContaining("Invalid priority range");

            // When/Then - max > 10
            assertThatThrownBy(() -> feedService.getFeedsByPriorityRange(1, 11))
                    .isInstanceOf(InvalidFeedException.class)
                    .hasMessageContaining("Invalid priority range");
        }

        @Test
        @DisplayName("Should get filtered feeds with pagination")
        void shouldGetFilteredFeedsWithPagination() {
            // Given
            FeedFilterParams filters = FeedFilterParams.builder()
                    .sourceId(1L)
                    .isActive(true)
                    .build();

            Pageable pageable = PageRequest.of(0, 20);
            List<Feed> feeds = Arrays.asList(testFeed);
            Page<Feed> feedPage = new PageImpl<>(feeds, pageable, 1);
            List<FeedResponseDto> responseDtos = Arrays.asList(testFeedResponseDto);

            when(feedRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(feedPage);
            when(feedMapper.toDtoList(feeds)).thenReturn(responseDtos);

            // When
            PagedResponseDto<FeedResponseDto> result = feedService.getFilteredFeeds(filters, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            verify(feedRepository).findAll(any(Specification.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Update Feed Tests")
    class UpdateFeedTests {

        @Test
        @DisplayName("Should update feed successfully")
        void shouldUpdateFeedSuccessfully() {
            // Given
            when(feedRepository.findById(1L)).thenReturn(Optional.of(testFeed));
            when(feedRepository.save(any(Feed.class))).thenReturn(testFeed);
            when(feedMapper.toDto(any(Feed.class))).thenReturn(testFeedResponseDto);
            doNothing().when(feedMapper).updateEntityFromDto(any(FeedRequestDto.class), any(Feed.class));

            // When
            FeedResponseDto result = feedService.updateFeed(1L, testFeedRequestDto);

            // Then
            assertThat(result).isNotNull();
            verify(feedRepository).findById(1L);
            verify(feedMapper).updateEntityFromDto(testFeedRequestDto, testFeed);
            verify(feedRepository).save(testFeed);
            // Note: existsByFeedUrl is not called when updating with the same URL
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent feed")
        void shouldThrowExceptionWhenUpdatingNonExistentFeed() {
            // Given
            when(feedRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> feedService.updateFeed(999L, testFeedRequestDto))
                    .isInstanceOf(FeedNotFoundException.class);

            verify(feedRepository).findById(999L);
            verify(feedRepository, never()).save(any(Feed.class));
        }

        @Test
        @DisplayName("Should throw exception when updating with duplicate URL")
        void shouldThrowExceptionWhenUpdatingWithDuplicateUrl() {
            // Given
            testFeedRequestDto.setFeedUrl("https://different.com/rss");
            when(feedRepository.findById(1L)).thenReturn(Optional.of(testFeed));
            when(feedRepository.existsByFeedUrl("https://different.com/rss")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> feedService.updateFeed(1L, testFeedRequestDto))
                    .isInstanceOf(InvalidFeedException.class)
                    .hasMessageContaining("Feed with URL already exists");
        }

        @Test
        @DisplayName("Should allow updating with same URL")
        void shouldAllowUpdatingWithSameUrl() {
            // Given
            when(feedRepository.findById(1L)).thenReturn(Optional.of(testFeed));
            when(feedRepository.save(any(Feed.class))).thenReturn(testFeed);
            when(feedMapper.toDto(any(Feed.class))).thenReturn(testFeedResponseDto);
            doNothing().when(feedMapper).updateEntityFromDto(any(FeedRequestDto.class), any(Feed.class));

            // When
            FeedResponseDto result = feedService.updateFeed(1L, testFeedRequestDto);

            // Then
            assertThat(result).isNotNull();
            verify(feedRepository, never()).existsByFeedUrl(anyString());
        }
    }

    @Nested
    @DisplayName("Delete Feed Tests")
    class DeleteFeedTests {

        @Test
        @DisplayName("Should delete feed successfully")
        void shouldDeleteFeedSuccessfully() {
            // Given
            when(feedRepository.existsById(1L)).thenReturn(true);
            doNothing().when(feedRepository).deleteById(1L);

            // When
            feedService.deleteFeed(1L);

            // Then
            verify(feedRepository).existsById(1L);
            verify(feedRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent feed")
        void shouldThrowExceptionWhenDeletingNonExistentFeed() {
            // Given
            when(feedRepository.existsById(999L)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> feedService.deleteFeed(999L))
                    .isInstanceOf(FeedNotFoundException.class);

            verify(feedRepository).existsById(999L);
            verify(feedRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("Activate/Deactivate Feed Tests")
    class ActivateDeactivateFeedTests {

        @Test
        @DisplayName("Should activate feed successfully")
        void shouldActivateFeedSuccessfully() {
            // Given
            testFeed.setIsActive(false);
            testFeed.setFailureCount(5);

            when(feedRepository.findById(1L)).thenReturn(Optional.of(testFeed));
            when(feedRepository.save(any(Feed.class))).thenReturn(testFeed);
            when(feedMapper.toDto(any(Feed.class))).thenReturn(testFeedResponseDto);

            // When
            FeedResponseDto result = feedService.activateFeed(1L);

            // Then
            assertThat(result).isNotNull();
            verify(feedRepository).findById(1L);

            ArgumentCaptor<Feed> feedCaptor = ArgumentCaptor.forClass(Feed.class);
            verify(feedRepository).save(feedCaptor.capture());
            Feed savedFeed = feedCaptor.getValue();
            assertThat(savedFeed.getIsActive()).isTrue();
            assertThat(savedFeed.getFailureCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should deactivate feed successfully")
        void shouldDeactivateFeedSuccessfully() {
            // Given
            when(feedRepository.findById(1L)).thenReturn(Optional.of(testFeed));
            when(feedRepository.save(any(Feed.class))).thenReturn(testFeed);
            when(feedMapper.toDto(any(Feed.class))).thenReturn(testFeedResponseDto);

            // When
            FeedResponseDto result = feedService.deactivateFeed(1L);

            // Then
            assertThat(result).isNotNull();
            verify(feedRepository).findById(1L);

            ArgumentCaptor<Feed> feedCaptor = ArgumentCaptor.forClass(Feed.class);
            verify(feedRepository).save(feedCaptor.capture());
            Feed savedFeed = feedCaptor.getValue();
            assertThat(savedFeed.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("Should throw exception when activating non-existent feed")
        void shouldThrowExceptionWhenActivatingNonExistentFeed() {
            // Given
            when(feedRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> feedService.activateFeed(999L))
                    .isInstanceOf(FeedNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Crawl Tracking Tests")
    class CrawlTrackingTests {

        @Test
        @DisplayName("Should record successful crawl")
        void shouldRecordSuccessfulCrawl() {
            // Given
            when(feedRepository.findById(1L)).thenReturn(Optional.of(testFeed));
            when(feedRepository.save(any(Feed.class))).thenReturn(testFeed);

            // When
            feedService.recordSuccessfulCrawl(1L);

            // Then
            verify(feedRepository).findById(1L);
            verify(feedRepository).save(testFeed);
            // Note: recordSuccessfulCrawl() is called on the feed entity
        }

        @Test
        @DisplayName("Should record failed crawl")
        void shouldRecordFailedCrawl() {
            // Given
            String errorMessage = "Connection timeout";
            when(feedRepository.findById(1L)).thenReturn(Optional.of(testFeed));
            when(feedRepository.save(any(Feed.class))).thenReturn(testFeed);

            // When
            feedService.recordFailedCrawl(1L, errorMessage);

            // Then
            verify(feedRepository).findById(1L);
            verify(feedRepository).save(testFeed);
        }

        @Test
        @DisplayName("Should throw exception when recording crawl for non-existent feed")
        void shouldThrowExceptionWhenRecordingCrawlForNonExistentFeed() {
            // Given
            when(feedRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> feedService.recordSuccessfulCrawl(999L))
                    .isInstanceOf(FeedNotFoundException.class);

            assertThatThrownBy(() -> feedService.recordFailedCrawl(999L, "Error"))
                    .isInstanceOf(FeedNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Bulk Operations Tests")
    class BulkOperationsTests {

        @Test
        @DisplayName("Should bulk activate feeds successfully")
        void shouldBulkActivateFeedsSuccessfully() {
            // Given
            Feed feed1 = new Feed();
            feed1.setId(1L);
            feed1.setIsActive(false);
            feed1.setFailureCount(3);

            Feed feed2 = new Feed();
            feed2.setId(2L);
            feed2.setIsActive(false);
            feed2.setFailureCount(5);

            List<Long> feedIds = Arrays.asList(1L, 2L);
            List<Feed> feeds = Arrays.asList(feed1, feed2);

            when(feedRepository.findAllById(feedIds)).thenReturn(feeds);
            when(feedRepository.saveAll(feeds)).thenReturn(feeds);

            // When
            feedService.bulkActivateFeeds(feedIds);

            // Then
            verify(feedRepository).findAllById(feedIds);

            ArgumentCaptor<List<Feed>> feedsCaptor = ArgumentCaptor.forClass(List.class);
            verify(feedRepository).saveAll(feedsCaptor.capture());
            List<Feed> savedFeeds = feedsCaptor.getValue();

            assertThat(savedFeeds).hasSize(2);
            assertThat(savedFeeds).allMatch(feed -> feed.getIsActive());
            assertThat(savedFeeds).allMatch(feed -> feed.getFailureCount() == 0);
        }

        @Test
        @DisplayName("Should bulk deactivate feeds successfully")
        void shouldBulkDeactivateFeedsSuccessfully() {
            // Given
            Feed feed1 = new Feed();
            feed1.setId(1L);
            feed1.setIsActive(true);

            Feed feed2 = new Feed();
            feed2.setId(2L);
            feed2.setIsActive(true);

            List<Long> feedIds = Arrays.asList(1L, 2L);
            List<Feed> feeds = Arrays.asList(feed1, feed2);

            when(feedRepository.findAllById(feedIds)).thenReturn(feeds);
            when(feedRepository.saveAll(feeds)).thenReturn(feeds);

            // When
            feedService.bulkDeactivateFeeds(feedIds);

            // Then
            verify(feedRepository).findAllById(feedIds);

            ArgumentCaptor<List<Feed>> feedsCaptor = ArgumentCaptor.forClass(List.class);
            verify(feedRepository).saveAll(feedsCaptor.capture());
            List<Feed> savedFeeds = feedsCaptor.getValue();

            assertThat(savedFeeds).hasSize(2);
            assertThat(savedFeeds).allMatch(feed -> !feed.getIsActive());
        }
    }
}