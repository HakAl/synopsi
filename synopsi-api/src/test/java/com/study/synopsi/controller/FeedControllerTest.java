package com.study.synopsi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.synopsi.dto.FeedRequestDto;
import com.study.synopsi.dto.FeedResponseDto;
import com.study.synopsi.dto.PagedResponseDto;
import com.study.synopsi.dto.filter.FeedFilterParams;
import com.study.synopsi.exception.FeedNotFoundException;
import com.study.synopsi.exception.GlobalExceptionHandler;
import com.study.synopsi.exception.InvalidFeedException;
import com.study.synopsi.model.Feed;
import com.study.synopsi.service.FeedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeedController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("FeedController Tests")
class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FeedService feedService;

    private FeedRequestDto testFeedRequestDto;
    private FeedResponseDto testFeedResponseDto;

    @BeforeEach
    void setUp() {
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
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ========================================================================
    // CRUD OPERATION TESTS
    // ========================================================================

    @Nested
    @DisplayName("GET /api/feeds - Get Feeds Tests")
    class GetFeedsTests {

        @Test
        @DisplayName("Should get filtered feeds with pagination")
        void shouldGetFilteredFeedsWithPagination() throws Exception {
            // Given
            List<FeedResponseDto> feeds = Arrays.asList(testFeedResponseDto);
            PagedResponseDto<FeedResponseDto> pagedResponse = new PagedResponseDto<>();
            pagedResponse.setContent(feeds);
            pagedResponse.setTotalElements(1L);
            pagedResponse.setTotalPages(1);
            pagedResponse.setPageSize(20);
            pagedResponse.setPageNumber(0);

            when(feedService.getFilteredFeeds(any(FeedFilterParams.class), any(Pageable.class)))
                    .thenReturn(pagedResponse);

            // When/Then
            mockMvc.perform(get("/api/feeds")
                            .param("sourceId", "1")
                            .param("isActive", "true")
                            .param("page", "0")
                            .param("size", "20")
                            .param("sort", "priority", "desc")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].feedUrl").value("https://example.com/rss"))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));

            verify(feedService).getFilteredFeeds(any(FeedFilterParams.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should get feeds with all filter parameters")
        void shouldGetFeedsWithAllFilters() throws Exception {
            // Given
            PagedResponseDto<FeedResponseDto> pagedResponse = new PagedResponseDto<>();
            pagedResponse.setContent(Arrays.asList(testFeedResponseDto));

            when(feedService.getFilteredFeeds(any(FeedFilterParams.class), any(Pageable.class)))
                    .thenReturn(pagedResponse);

            // When/Then
            mockMvc.perform(get("/api/feeds")
                            .param("sourceId", "1")
                            .param("feedType", "RSS")
                            .param("isActive", "true")
                            .param("topicId", "1")
                            .param("minPriority", "5")
                            .param("maxPriority", "10")
                            .param("minFailureCount", "0")
                            .param("maxFailureCount", "3")
                            .param("searchTerm", "test")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(feedService).getFilteredFeeds(any(FeedFilterParams.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("GET /api/feeds/{id} - Get Feed by ID Tests")
    class GetFeedByIdTests {

        @Test
        @DisplayName("Should get feed by ID successfully")
        void shouldGetFeedByIdSuccessfully() throws Exception {
            // Given
            when(feedService.getFeedById(1L)).thenReturn(testFeedResponseDto);

            // When/Then
            mockMvc.perform(get("/api/feeds/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.feedUrl").value("https://example.com/rss"))
                    .andExpect(jsonPath("$.title").value("Test Feed"))
                    .andExpect(jsonPath("$.priority").value(5));

            verify(feedService).getFeedById(1L);
        }

        @Test
        @DisplayName("Should return 404 when feed not found")
        void shouldReturn404WhenFeedNotFound() throws Exception {
            // Given
            when(feedService.getFeedById(999L)).thenThrow(new FeedNotFoundException(999L));

            // When/Then
            mockMvc.perform(get("/api/feeds/999")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(feedService).getFeedById(999L);
        }
    }

    @Nested
    @DisplayName("POST /api/feeds - Create Feed Tests")
    class CreateFeedTests {

        @Test
        @DisplayName("Should create feed successfully")
        void shouldCreateFeedSuccessfully() throws Exception {
            // Given
            when(feedService.createFeed(any(FeedRequestDto.class))).thenReturn(testFeedResponseDto);

            // When/Then
            mockMvc.perform(post("/api/feeds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testFeedRequestDto)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.feedUrl").value("https://example.com/rss"))
                    .andExpect(jsonPath("$.title").value("Test Feed"));

            verify(feedService).createFeed(any(FeedRequestDto.class));
        }

        @Test
        @DisplayName("Should return 400 for invalid feed URL")
        void shouldReturn400ForInvalidFeedUrl() throws Exception {
            // Given
            testFeedRequestDto.setFeedUrl("invalid-url");
            // Note: @Pattern validation on DTO will catch this before service is called

            // When/Then
            mockMvc.perform(post("/api/feeds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testFeedRequestDto)))
                    .andExpect(status().isBadRequest());

            // Service should NOT be called due to validation failure
            verify(feedService, never()).createFeed(any(FeedRequestDto.class));
        }

        @Test
        @DisplayName("Should return 400 for duplicate feed URL")
        void shouldReturn400ForDuplicateFeedUrl() throws Exception {
            // Given
            when(feedService.createFeed(any(FeedRequestDto.class)))
                    .thenThrow(new InvalidFeedException("Feed with URL already exists"));

            // When/Then
            mockMvc.perform(post("/api/feeds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testFeedRequestDto)))
                    .andExpect(status().isBadRequest());

            verify(feedService).createFeed(any(FeedRequestDto.class));
        }

        @Test
        @DisplayName("Should return 400 for invalid priority")
        void shouldReturn400ForInvalidPriority() throws Exception {
            // Given
            testFeedRequestDto.setPriority(11);
            when(feedService.createFeed(any(FeedRequestDto.class)))
                    .thenThrow(new InvalidFeedException("Priority must be between 1 and 10"));

            // When/Then
            mockMvc.perform(post("/api/feeds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testFeedRequestDto)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/feeds/{id} - Update Feed Tests")
    class UpdateFeedTests {

        @Test
        @DisplayName("Should update feed successfully")
        void shouldUpdateFeedSuccessfully() throws Exception {
            // Given
            when(feedService.updateFeed(anyLong(), any(FeedRequestDto.class)))
                    .thenReturn(testFeedResponseDto);

            // When/Then
            mockMvc.perform(put("/api/feeds/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testFeedRequestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.feedUrl").value("https://example.com/rss"));

            verify(feedService).updateFeed(eq(1L), any(FeedRequestDto.class));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent feed")
        void shouldReturn404WhenUpdatingNonExistentFeed() throws Exception {
            // Given
            when(feedService.updateFeed(anyLong(), any(FeedRequestDto.class)))
                    .thenThrow(new FeedNotFoundException(999L));

            // When/Then
            mockMvc.perform(put("/api/feeds/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testFeedRequestDto)))
                    .andExpect(status().isNotFound());

            verify(feedService).updateFeed(eq(999L), any(FeedRequestDto.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/feeds/{id} - Delete Feed Tests")
    class DeleteFeedTests {

        @Test
        @DisplayName("Should delete feed successfully")
        void shouldDeleteFeedSuccessfully() throws Exception {
            // Given
            doNothing().when(feedService).deleteFeed(1L);

            // When/Then
            mockMvc.perform(delete("/api/feeds/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNoContent());

            verify(feedService).deleteFeed(1L);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent feed")
        void shouldReturn404WhenDeletingNonExistentFeed() throws Exception {
            // Given
            doThrow(new FeedNotFoundException(999L)).when(feedService).deleteFeed(999L);

            // When/Then
            mockMvc.perform(delete("/api/feeds/999")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(feedService).deleteFeed(999L);
        }
    }

    // ========================================================================
    // QUERY ENDPOINT TESTS
    // ========================================================================

    @Nested
    @DisplayName("Query Endpoint Tests")
    class QueryEndpointTests {

        @Test
        @DisplayName("Should get feeds by source ID")
        void shouldGetFeedsBySourceId() throws Exception {
            // Given
            List<FeedResponseDto> feeds = Arrays.asList(testFeedResponseDto);
            when(feedService.getFeedsBySourceId(1L)).thenReturn(feeds);

            // When/Then
            mockMvc.perform(get("/api/feeds/source/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].sourceId").value(1));

            verify(feedService).getFeedsBySourceId(1L);
        }

        @Test
        @DisplayName("Should get active feeds")
        void shouldGetActiveFeeds() throws Exception {
            // Given
            List<FeedResponseDto> feeds = Arrays.asList(testFeedResponseDto);
            when(feedService.getActiveFeeds()).thenReturn(feeds);

            // When/Then
            mockMvc.perform(get("/api/feeds/active")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].isActive").value(true));

            verify(feedService).getActiveFeeds();
        }

        @Test
        @DisplayName("Should get feeds needing crawl")
        void shouldGetFeedsNeedingCrawl() throws Exception {
            // Given
            List<FeedResponseDto> feeds = Arrays.asList(testFeedResponseDto);
            when(feedService.getFeedsNeedingCrawl()).thenReturn(feeds);

            // When/Then
            mockMvc.perform(get("/api/feeds/needs-crawl")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(feedService).getFeedsNeedingCrawl();
        }

        @Test
        @DisplayName("Should get feeds by priority range")
        void shouldGetFeedsByPriorityRange() throws Exception {
            // Given
            List<FeedResponseDto> feeds = Arrays.asList(testFeedResponseDto);
            when(feedService.getFeedsByPriorityRange(5, 10)).thenReturn(feeds);

            // When/Then
            mockMvc.perform(get("/api/feeds/priority")
                            .param("min", "5")
                            .param("max", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(feedService).getFeedsByPriorityRange(5, 10);
        }

        @Test
        @DisplayName("Should get feeds by type")
        void shouldGetFeedsByType() throws Exception {
            // Given
            List<FeedResponseDto> feeds = Arrays.asList(testFeedResponseDto);
            when(feedService.getFeedsByType(Feed.FeedType.RSS)).thenReturn(feeds);

            // When/Then
            mockMvc.perform(get("/api/feeds/type/RSS")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].feedType").value("RSS"));

            verify(feedService).getFeedsByType(Feed.FeedType.RSS);
        }

        @Test
        @DisplayName("Should get all feeds (deprecated)")
        void shouldGetAllFeedsDeprecated() throws Exception {
            // Given
            List<FeedResponseDto> feeds = Arrays.asList(testFeedResponseDto);
            when(feedService.getAllFeeds()).thenReturn(feeds);

            // When/Then
            mockMvc.perform(get("/api/feeds/all")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(feedService).getAllFeeds();
        }
    }

    // ========================================================================
    // STATUS MANAGEMENT TESTS
    // ========================================================================

    @Nested
    @DisplayName("Status Management Tests")
    class StatusManagementTests {

        @Test
        @DisplayName("Should activate feed successfully")
        void shouldActivateFeedSuccessfully() throws Exception {
            // Given
            testFeedResponseDto.setIsActive(true);
            when(feedService.activateFeed(1L)).thenReturn(testFeedResponseDto);

            // When/Then
            mockMvc.perform(patch("/api/feeds/1/activate")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.isActive").value(true));

            verify(feedService).activateFeed(1L);
        }

        @Test
        @DisplayName("Should deactivate feed successfully")
        void shouldDeactivateFeedSuccessfully() throws Exception {
            // Given
            testFeedResponseDto.setIsActive(false);
            when(feedService.deactivateFeed(1L)).thenReturn(testFeedResponseDto);

            // When/Then
            mockMvc.perform(patch("/api/feeds/1/deactivate")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.isActive").value(false));

            verify(feedService).deactivateFeed(1L);
        }

        @Test
        @DisplayName("Should return 404 when activating non-existent feed")
        void shouldReturn404WhenActivatingNonExistentFeed() throws Exception {
            // Given
            when(feedService.activateFeed(999L)).thenThrow(new FeedNotFoundException(999L));

            // When/Then
            mockMvc.perform(patch("/api/feeds/999/activate")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(feedService).activateFeed(999L);
        }

        @Test
        @DisplayName("Should bulk activate feeds successfully")
        void shouldBulkActivateFeedsSuccessfully() throws Exception {
            // Given
            List<Long> feedIds = Arrays.asList(1L, 2L, 3L);
            doNothing().when(feedService).bulkActivateFeeds(feedIds);

            // When/Then
            mockMvc.perform(post("/api/feeds/bulk/activate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(feedIds)))
                    .andDo(print())
                    .andExpect(status().isOk());

            verify(feedService).bulkActivateFeeds(feedIds);
        }

        @Test
        @DisplayName("Should bulk deactivate feeds successfully")
        void shouldBulkDeactivateFeedsSuccessfully() throws Exception {
            // Given
            List<Long> feedIds = Arrays.asList(1L, 2L, 3L);
            doNothing().when(feedService).bulkDeactivateFeeds(feedIds);

            // When/Then
            mockMvc.perform(post("/api/feeds/bulk/deactivate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(feedIds)))
                    .andDo(print())
                    .andExpect(status().isOk());

            verify(feedService).bulkDeactivateFeeds(feedIds);
        }
    }

    // ========================================================================
    // WORKER INTEGRATION TESTS
    // ========================================================================

    @Nested
    @DisplayName("Worker Integration Tests")
    class WorkerIntegrationTests {

        @Test
        @DisplayName("Should record successful crawl")
        void shouldRecordSuccessfulCrawl() throws Exception {
            // Given
            doNothing().when(feedService).recordSuccessfulCrawl(1L);

            // When/Then
            mockMvc.perform(post("/api/feeds/1/crawl/success")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk());

            verify(feedService).recordSuccessfulCrawl(1L);
        }

        @Test
        @DisplayName("Should record failed crawl")
        void shouldRecordFailedCrawl() throws Exception {
            // Given
            String errorMessage = "Connection timeout";
            FeedController.CrawlFailureRequest request =
                    new FeedController.CrawlFailureRequest(errorMessage);
            doNothing().when(feedService).recordFailedCrawl(1L, errorMessage);

            // When/Then
            mockMvc.perform(post("/api/feeds/1/crawl/failure")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk());

            verify(feedService).recordFailedCrawl(1L, errorMessage);
        }

        @Test
        @DisplayName("Should return 404 when recording crawl for non-existent feed")
        void shouldReturn404WhenRecordingCrawlForNonExistentFeed() throws Exception {
            // Given
            doThrow(new FeedNotFoundException(999L)).when(feedService).recordSuccessfulCrawl(999L);

            // When/Then
            mockMvc.perform(post("/api/feeds/999/crawl/success")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(feedService).recordSuccessfulCrawl(999L);
        }

        @Test
        @DisplayName("Should handle failed crawl with error message")
        void shouldHandleFailedCrawlWithErrorMessage() throws Exception {
            // Given
            String errorMessage = "Feed parse error: Invalid XML";
            FeedController.CrawlFailureRequest request =
                    new FeedController.CrawlFailureRequest(errorMessage);
            doNothing().when(feedService).recordFailedCrawl(1L, errorMessage);

            // When/Then
            mockMvc.perform(post("/api/feeds/1/crawl/failure")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(feedService).recordFailedCrawl(1L, errorMessage);
        }
    }
}