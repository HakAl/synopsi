package com.study.synopsi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.synopsi.config.JwtAuthenticationFilter;
import com.study.synopsi.config.JwtUtil;
import com.study.synopsi.dto.ArticleStatsResponse;
import com.study.synopsi.model.Article;
import com.study.synopsi.service.AuthService;
import com.study.synopsi.service.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatsController.class)
@AutoConfigureMockMvc(addFilters = false)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService statsService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private AuthService authService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Test
    void getArticleStats_whenCalled_shouldReturn200WithStatsStructure() throws Exception {
        // Arrange
        Map<String, Long> countByStatus = new HashMap<>();
        countByStatus.put(Article.ArticleStatus.PENDING.name(), 10L);
        countByStatus.put(Article.ArticleStatus.SUMMARIZED.name(), 5L);
        countByStatus.put(Article.ArticleStatus.PROCESSING.name(), 3L);
        countByStatus.put(Article.ArticleStatus.ARCHIVED.name(), 2L);

        LocalDateTime now = LocalDateTime.now();
        ArticleStatsResponse statsResponse = ArticleStatsResponse.builder()
                .totalCount(20L)
                .countByStatus(countByStatus)
                .lastUpdated(now)
                .build();

        when(statsService.getArticleStats()).thenReturn(statsResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/stats/articles"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount", is(20)))
                .andExpect(jsonPath("$.countByStatus.PENDING", is(10)))
                .andExpect(jsonPath("$.countByStatus.SUMMARIZED", is(5)))
                .andExpect(jsonPath("$.countByStatus.PROCESSING", is(3)))
                .andExpect(jsonPath("$.countByStatus.ARCHIVED", is(2)))
                .andExpect(jsonPath("$.lastUpdated").exists());
    }

    @Test
    void getArticleStats_whenEmptyDatabase_shouldReturn200WithZeroCounts() throws Exception {
        // Arrange
        Map<String, Long> countByStatus = new HashMap<>();
        countByStatus.put(Article.ArticleStatus.PENDING.name(), 0L);
        countByStatus.put(Article.ArticleStatus.SUMMARIZED.name(), 0L);
        countByStatus.put(Article.ArticleStatus.PROCESSING.name(), 0L);
        countByStatus.put(Article.ArticleStatus.ARCHIVED.name(), 0L);

        ArticleStatsResponse statsResponse = ArticleStatsResponse.builder()
                .totalCount(0L)
                .countByStatus(countByStatus)
                .lastUpdated(LocalDateTime.now())
                .build();

        when(statsService.getArticleStats()).thenReturn(statsResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/stats/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount", is(0)))
                .andExpect(jsonPath("$.countByStatus.PENDING", is(0)))
                .andExpect(jsonPath("$.countByStatus.SUMMARIZED", is(0)));
    }

    @Test
    void getArticleStats_whenServiceThrowsException_shouldReturnInternalServerError() throws Exception {
        // Arrange
        when(statsService.getArticleStats()).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/stats/articles"))
                .andExpect(status().isInternalServerError());
    }
}
