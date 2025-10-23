package com.study.synopsi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.synopsi.dto.ArticleRequestDto;
import com.study.synopsi.dto.ArticleResponseDto;
import com.study.synopsi.dto.PagedResponseDto;
import com.study.synopsi.dto.filter.ArticleFilterParams;
import com.study.synopsi.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArticleController.class)
public class ArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleService articleService;

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
        mockMvc.perform(get("/api/articles")
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
        mockMvc.perform(get("/api/articles")
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
        mockMvc.perform(get("/api/articles/{id}", 1L)
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
        mockMvc.perform(post("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.summary").value("New Summary"));
    }
}