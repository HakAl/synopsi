package com.study.synopsi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.synopsi.config.JwtAuthenticationFilter;
import com.study.synopsi.config.JwtUtil;
import com.study.synopsi.dto.ArticleRequestDto;
import com.study.synopsi.dto.ArticleResponseDto;
import com.study.synopsi.dto.PagedResponseDto;
import com.study.synopsi.dto.filter.ArticleFilterParams;
import com.study.synopsi.exception.GlobalExceptionHandler;
import com.study.synopsi.service.ArticleService;
import com.study.synopsi.service.AuthService;
import com.study.synopsi.exception.ArticleNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArticleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
public class ArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArticleService articleService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private AuthService authService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Autowired
    private ObjectMapper objectMapper;
    @Test
    void whenGetAllArticles_thenReturnListOfArticles() throws Exception {
        // Given: Create article DTO
        ArticleResponseDto article = ArticleResponseDto.builder()
                .id(1L)
                .title("Test Title")
                .summary("Test Summary")
                .build();

        List<ArticleResponseDto> articles = Collections.singletonList(article);

        // Create paginated response
        PagedResponseDto<ArticleResponseDto> pagedResponse = new PagedResponseDto<>();
        pagedResponse.setContent(articles);
        pagedResponse.setPageNumber(0);
        pagedResponse.setPageSize(20);
        pagedResponse.setTotalElements(1L);
        pagedResponse.setTotalPages(1);
        pagedResponse.setFirst(true);
        pagedResponse.setLast(true);
        pagedResponse.setEmpty(false);
        pagedResponse.setNumberOfElements(1);

        // When: Mock the service to return paginated response
        when(articleService.getFilteredArticles(any(ArticleFilterParams.class), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // Then: Verify the response structure
        mockMvc.perform(get("/api/v1/articles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].title").value("Test Title"))
                .andExpect(jsonPath("$.content[0].summary").value("Test Summary"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void whenGetAllArticles_withFilters_thenReturnFilteredArticles() throws Exception {
        // Given: Create article DTO
        ArticleResponseDto article = ArticleResponseDto.builder()
                .id(1L)
                .title("Filtered Title")
                .summary("Filtered Summary")
                .build();

        List<ArticleResponseDto> articles = Collections.singletonList(article);

        // Create paginated response
        PagedResponseDto<ArticleResponseDto> pagedResponse = new PagedResponseDto<>();
        pagedResponse.setContent(articles);
        pagedResponse.setPageNumber(0);
        pagedResponse.setPageSize(10);
        pagedResponse.setTotalElements(1L);
        pagedResponse.setTotalPages(1);
        pagedResponse.setFirst(true);
        pagedResponse.setLast(true);
        pagedResponse.setEmpty(false);
        pagedResponse.setNumberOfElements(1);

        // When: Mock the service
        when(articleService.getFilteredArticles(any(ArticleFilterParams.class), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // Then: Test with query parameters
        mockMvc.perform(get("/api/v1/articles")
                        .param("status", "SUMMARIZED")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.pageSize").value(10));
    }

    @Test
    void whenGetArticleById_withValidId_thenReturnArticle() throws Exception {
        // Given: Use the builder
        ArticleResponseDto article = ArticleResponseDto.builder()
                .id(1L)
                .title("Test Title")
                .summary("Test Summary")
                .build();

        // When
        when(articleService.getArticleById(1L)).thenReturn(article);

        // Then
        mockMvc.perform(get("/api/v1/articles/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andExpect(jsonPath("$.summary").value("Test Summary"));
    }

    @Test
    void whenCreateArticle_withValidRequest_thenCreateArticle() throws Exception {
        // Given: Use the builder for both request and response DTOs with all required fields
        ArticleRequestDto requestDto = ArticleRequestDto.builder()
                .title("New Title")
                .originalUrl("https://example.com/article")
                .content("New Content")
                .feedId(1L)
                .build();

        ArticleResponseDto createdArticle = ArticleResponseDto.builder()
                .id(1L)
                .title("New Title")
                .summary("New Summary")
                .build();

        // When
        when(articleService.createArticle(any(ArticleRequestDto.class))).thenReturn(createdArticle);

        // Then
        mockMvc.perform(post("/api/v1/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.summary").value("New Summary"));
    }

    // ============================================
// ERROR HANDLING TESTS
// ============================================

    @Test
    void whenGetArticleById_withInvalidId_thenReturn404() throws Exception {
        // Given
        Long invalidId = 999L;
        when(articleService.getArticleById(invalidId))
                .thenThrow(new ArticleNotFoundException(invalidId));

        // When/Then
        mockMvc.perform(get("/api/v1/articles/{id}", invalidId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Article not found with id: 999"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

// ============================================
// VALIDATION TESTS
// ============================================

    @Test
    void whenCreateArticle_withMissingTitle_thenReturn400() throws Exception {
        // Given: Request with missing title
        ArticleRequestDto requestDto = ArticleRequestDto.builder()
                .originalUrl("https://example.com/article")
                .content("Content")
                .feedId(1L)
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void whenCreateArticle_withInvalidUrl_thenReturn400() throws Exception {
        // Given: Request with invalid URL
        ArticleRequestDto requestDto = ArticleRequestDto.builder()
                .title("Test Title")
                .originalUrl("not-a-valid-url")
                .content("Content")
                .feedId(1L)
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.originalUrl").exists());
    }

    @Test
    void whenCreateArticle_withMissingFeedId_thenReturn400() throws Exception {
        // Given: Request without feedId
        ArticleRequestDto requestDto = ArticleRequestDto.builder()
                .title("Test Title")
                .originalUrl("https://example.com/article")
                .content("Content")
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.feedId").exists());
    }

    @Test
    void whenCreateArticle_withInvalidImageUrl_thenReturn400() throws Exception {
        // Given: Request with invalid imageUrl
        ArticleRequestDto requestDto = ArticleRequestDto.builder()
                .title("Test Title")
                .originalUrl("https://example.com/article")
                .content("Content")
                .feedId(1L)
                .imageUrl("invalid-image-url")
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.imageUrl").exists());
    }

// ============================================
// FILTER VALIDATION TESTS
// ============================================

    @Test
    void whenGetAllArticles_withInvalidDateRange_thenReturn400() throws Exception {
        // Given: startDate is after endDate
        mockMvc.perform(get("/api/v1/articles")
                        .param("startDate", "2025-10-23T00:00:00")
                        .param("endDate", "2025-10-01T00:00:00")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("startDate must be before or equal to endDate"));
    }

    @Test
    void whenGetAllArticles_withInvalidStatus_thenReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/articles")
                        .param("status", "INVALID_STATUS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.status").exists());
    }

// ============================================
// FILTER COMBINATION TESTS
// ============================================

    @Test
    void whenGetAllArticles_withDateRangeFilter_thenReturnFilteredArticles() throws Exception {
        // Given
        ArticleResponseDto article = ArticleResponseDto.builder()
                .id(1L)
                .title("Recent Article")
                .publicationDate(LocalDateTime.of(2025, 10, 15, 10, 0))
                .build();

        PagedResponseDto<ArticleResponseDto> pagedResponse = new PagedResponseDto<>();
        pagedResponse.setContent(Collections.singletonList(article));
        pagedResponse.setPageNumber(0);
        pagedResponse.setPageSize(20);
        pagedResponse.setTotalElements(1L);
        pagedResponse.setTotalPages(1);

        when(articleService.getFilteredArticles(any(ArticleFilterParams.class), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/articles")
                        .param("startDate", "2025-10-01T00:00:00")
                        .param("endDate", "2025-10-23T23:59:59")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    void whenGetAllArticles_withSearchTerm_thenReturnMatchingArticles() throws Exception {
        // Given
        ArticleResponseDto article = ArticleResponseDto.builder()
                .id(1L)
                .title("Climate Change Impact")
                .content("Article about climate")
                .build();

        PagedResponseDto<ArticleResponseDto> pagedResponse = new PagedResponseDto<>();
        pagedResponse.setContent(Collections.singletonList(article));
        pagedResponse.setPageNumber(0);
        pagedResponse.setPageSize(20);
        pagedResponse.setTotalElements(1L);
        pagedResponse.setTotalPages(1);

        when(articleService.getFilteredArticles(any(ArticleFilterParams.class), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/articles")
                        .param("searchTerm", "climate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Climate Change Impact"));
    }

    @Test
    void whenGetAllArticles_withLanguageFilter_thenReturnFilteredArticles() throws Exception {
        // Given
        ArticleResponseDto article = ArticleResponseDto.builder()
                .id(1L)
                .title("English Article")
                .language("en")
                .build();

        PagedResponseDto<ArticleResponseDto> pagedResponse = new PagedResponseDto<>();
        pagedResponse.setContent(Collections.singletonList(article));
        pagedResponse.setPageNumber(0);
        pagedResponse.setPageSize(20);
        pagedResponse.setTotalElements(1L);
        pagedResponse.setTotalPages(1);

        when(articleService.getFilteredArticles(any(ArticleFilterParams.class), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/articles")
                        .param("language", "en")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].language").value("en"));
    }

    @Test
    void whenGetAllArticles_withMultipleFilters_thenReturnFilteredArticles() throws Exception {
        // Given
        ArticleResponseDto article = ArticleResponseDto.builder()
                .id(1L)
                .title("BBC Climate Article")
                .sourceName("BBC")
                .status("SUMMARIZED")
                .language("en")
                .build();

        PagedResponseDto<ArticleResponseDto> pagedResponse = new PagedResponseDto<>();
        pagedResponse.setContent(Collections.singletonList(article));
        pagedResponse.setPageNumber(0);
        pagedResponse.setPageSize(20);
        pagedResponse.setTotalElements(1L);
        pagedResponse.setTotalPages(1);

        when(articleService.getFilteredArticles(any(ArticleFilterParams.class), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // When/Then: Combine status, source, language, and searchTerm
        mockMvc.perform(get("/api/v1/articles")
                        .param("status", "SUMMARIZED")
                        .param("source", "BBC")
                        .param("language", "en")
                        .param("searchTerm", "climate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sourceName").value("BBC"))
                .andExpect(jsonPath("$.content[0].status").value("SUMMARIZED"));
    }

// ============================================
// PAGINATION TESTS
// ============================================

    @Test
    void whenGetAllArticles_withEmptyResults_thenReturnEmptyPage() throws Exception {
        // Given
        PagedResponseDto<ArticleResponseDto> pagedResponse = new PagedResponseDto<>();
        pagedResponse.setContent(Collections.emptyList());
        pagedResponse.setPageNumber(0);
        pagedResponse.setPageSize(20);
        pagedResponse.setTotalElements(0L);
        pagedResponse.setTotalPages(0);
        pagedResponse.setFirst(true);
        pagedResponse.setLast(true);
        pagedResponse.setEmpty(true);
        pagedResponse.setNumberOfElements(0);

        when(articleService.getFilteredArticles(any(ArticleFilterParams.class), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/articles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.empty").value(true));
    }

    @Test
    void whenGetAllArticles_withCustomPageSize_thenReturnRequestedSize() throws Exception {
        // Given
        PagedResponseDto<ArticleResponseDto> pagedResponse = new PagedResponseDto<>();
        pagedResponse.setContent(Collections.emptyList());
        pagedResponse.setPageNumber(0);
        pagedResponse.setPageSize(5);
        pagedResponse.setTotalElements(0L);
        pagedResponse.setTotalPages(0);

        when(articleService.getFilteredArticles(any(ArticleFilterParams.class), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/articles")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(5));
    }

// ============================================
// SORTING TESTS
// ============================================

    @Test
    void whenGetAllArticles_withAscendingSort_thenReturnSortedArticles() throws Exception {
        // Given
        PagedResponseDto<ArticleResponseDto> pagedResponse = new PagedResponseDto<>();
        pagedResponse.setContent(Collections.emptyList());
        pagedResponse.setPageNumber(0);
        pagedResponse.setPageSize(20);
        pagedResponse.setTotalElements(0L);
        pagedResponse.setTotalPages(0);

        when(articleService.getFilteredArticles(any(ArticleFilterParams.class), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/articles")
                        .param("sort", "publicationDate,asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void whenGetAllArticles_withTitleSort_thenReturnSortedArticles() throws Exception {
        // Given
        PagedResponseDto<ArticleResponseDto> pagedResponse = new PagedResponseDto<>();
        pagedResponse.setContent(Collections.emptyList());
        pagedResponse.setPageNumber(0);
        pagedResponse.setPageSize(20);
        pagedResponse.setTotalElements(0L);
        pagedResponse.setTotalPages(0);

        when(articleService.getFilteredArticles(any(ArticleFilterParams.class), any(Pageable.class)))
                .thenReturn(pagedResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/articles")
                        .param("sort", "title,desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}