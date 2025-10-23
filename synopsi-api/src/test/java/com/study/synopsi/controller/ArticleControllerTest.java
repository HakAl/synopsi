package com.study.synopsi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.synopsi.dto.ArticleRequestDto;
import com.study.synopsi.dto.ArticleResponseDto;
import com.study.synopsi.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
        // Given: Use the builder to create the DTO instance
        ArticleResponseDto article = ArticleResponseDto.builder()
                .id(1L)
                .title("Test Title")
                .summary("Test Summary")
                // ... add other mandatory fields with dummy data if needed
                .build();
        List<ArticleResponseDto> allArticles = Collections.singletonList(article);

        // When
        when(articleService.getAllArticles()).thenReturn(allArticles);

        // Then
        mockMvc.perform(get("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Test Title"))
                .andExpect(jsonPath("$[0].summary").value("Test Summary"));
    }

    @Test
    void whenGetArticleById_withValidId_thenReturnArticle() throws Exception {
        // Given: Use the builder
        ArticleResponseDto article = ArticleResponseDto.builder()
                .id(1L)
                .title("Test Title")
                .summary("Test Summary")
                // ... add other mandatory fields here
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
                // ... add other mandatory fields here
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