package com.study.synopsi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.synopsi.dto.ArticleInteractionDto;
import com.study.synopsi.dto.PersonalizedArticleDto;
import com.study.synopsi.dto.UserPreferenceDto;
import com.study.synopsi.dto.UserTopicInterestDto;
import com.study.synopsi.service.PersonalizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PersonalizationController.class)
class PersonalizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PersonalizationService personalizationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getPersonalizedFeed_shouldReturnPaginatedArticles() throws Exception {
        Long userId = 1L;
        PersonalizedArticleDto article = PersonalizedArticleDto.builder()
                .articleId(101L)
                .title("Test Article")
                .relevanceScore(0.85)
                .recommendationReason("Matches your interests")
                .build();

        Page<PersonalizedArticleDto> page = new PageImpl<>(List.of(article), PageRequest.of(0, 20), 1);

        when(personalizationService.getPersonalizedArticles(eq(userId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/personalization/feed/{userId}", userId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].articleId").value(101))
                .andExpect(jsonPath("$.content[0].title").value("Test Article"))
                .andExpect(jsonPath("$.content[0].relevanceScore").value(0.85))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getPersonalizedFeed_withCustomPagination_shouldReturnCorrectPage() throws Exception {
        Long userId = 1L;
        Page<PersonalizedArticleDto> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(2, 10), 15);

        when(personalizationService.getPersonalizedArticles(eq(userId), any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/personalization/feed/{userId}", userId)
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.number").value(2));
    }

    @Test
    void recordReadingInteraction_shouldReturnCreated() throws Exception {
        Long userId = 1L;
        ArticleInteractionDto interaction = ArticleInteractionDto.builder()
                .articleId(101L)
                .timeSpentSeconds(180)
                .completionPercentage(85)
                .build();

        doNothing().when(personalizationService).recordReadingInteraction(eq(userId), any(ArticleInteractionDto.class));

        mockMvc.perform(post("/api/v1/personalization/interactions/{userId}/read", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(interaction)))
                .andExpect(status().isCreated());
    }

    @Test
    void recordReadingInteraction_withInvalidData_shouldReturnBadRequest() throws Exception {
        Long userId = 1L;
        ArticleInteractionDto interaction = ArticleInteractionDto.builder()
                .articleId(null) // Invalid - required field
                .timeSpentSeconds(180)
                .build();

        mockMvc.perform(post("/api/v1/personalization/interactions/{userId}/read", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(interaction)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordFeedback_shouldReturnCreated() throws Exception {
        Long userId = 1L;
        ArticleInteractionDto interaction = ArticleInteractionDto.builder()
                .articleId(101L)
                .feedbackType(com.study.synopsi.model.UserArticleFeedback.FeedbackType.LIKED)
                .rating(5)
                .comment("Great article!")
                .build();

        doNothing().when(personalizationService).recordFeedback(eq(userId), any(ArticleInteractionDto.class));

        mockMvc.perform(post("/api/v1/personalization/interactions/{userId}/feedback", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(interaction)))
                .andExpect(status().isCreated());
    }

    @Test
    void recordFeedback_withSavedType_shouldReturnCreated() throws Exception {
        Long userId = 1L;
        ArticleInteractionDto interaction = ArticleInteractionDto.builder()
                .articleId(101L)
                .feedbackType(com.study.synopsi.model.UserArticleFeedback.FeedbackType.SAVED)
                .build();

        doNothing().when(personalizationService).recordFeedback(eq(userId), any(ArticleInteractionDto.class));

        mockMvc.perform(post("/api/v1/personalization/interactions/{userId}/feedback", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(interaction)))
                .andExpect(status().isCreated());
    }

    @Test
    void recordFeedback_withInvalidRating_shouldReturnBadRequest() throws Exception {
        Long userId = 1L;
        ArticleInteractionDto interaction = ArticleInteractionDto.builder()
                .articleId(101L)
                .feedbackType(com.study.synopsi.model.UserArticleFeedback.FeedbackType.LIKED)
                .rating(6) // Invalid - max is 5
                .build();

        mockMvc.perform(post("/api/v1/personalization/interactions/{userId}/feedback", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(interaction)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserPreferences_shouldReturnPreferencesList() throws Exception {
        Long userId = 1L;
        UserPreferenceDto preference = UserPreferenceDto.builder()
                .id(1L)
                .topicId(5L)
                .topicName("Technology")
                .interestLevel(com.study.synopsi.model.UserPreference.InterestLevel.HIGH)
                .isActive(true)
                .build();

        when(personalizationService.getUserPreferences(userId)).thenReturn(List.of(preference));

        mockMvc.perform(get("/api/v1/personalization/preferences/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].topicId").value(5))
                .andExpect(jsonPath("$[0].topicName").value("Technology"))
                .andExpect(jsonPath("$[0].interestLevel").value("HIGH"))
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    void getUserPreferences_withNoPreferences_shouldReturnEmptyList() throws Exception {
        Long userId = 1L;

        when(personalizationService.getUserPreferences(userId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/personalization/preferences/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void updateUserPreference_shouldReturnUpdatedPreference() throws Exception {
        Long userId = 1L;
        UserPreferenceDto inputDto = UserPreferenceDto.builder()
                .topicId(5L)
                .interestLevel(com.study.synopsi.model.UserPreference.InterestLevel.VERY_HIGH)
                .isActive(true)
                .build();

        UserPreferenceDto resultDto = UserPreferenceDto.builder()
                .id(1L)
                .topicId(5L)
                .topicName("Technology")
                .interestLevel(com.study.synopsi.model.UserPreference.InterestLevel.VERY_HIGH)
                .isActive(true)
                .build();

        when(personalizationService.updateUserPreference(eq(userId), any(UserPreferenceDto.class))).thenReturn(resultDto);

        mockMvc.perform(put("/api/v1/personalization/preferences/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.topicId").value(5))
                .andExpect(jsonPath("$.interestLevel").value("VERY_HIGH"));
    }

    @Test
    void updateUserPreference_withInvalidData_shouldReturnBadRequest() throws Exception {
        Long userId = 1L;
        UserPreferenceDto invalidDto = UserPreferenceDto.builder()
                .topicId(null) // Invalid - required field
                .interestLevel(com.study.synopsi.model.UserPreference.InterestLevel.HIGH)
                .build();

        mockMvc.perform(put("/api/v1/personalization/preferences/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getInferredInterests_shouldReturnInterestsList() throws Exception {
        Long userId = 1L;
        UserTopicInterestDto interest = UserTopicInterestDto.builder()
                .topicId(5L)
                .topicName("Technology")
                .articlesRead(15L)
                .articlesLiked(8L)
                .explicitInterestLevel("HIGH")
                .inferredInterestScore(0.75)
                .build();

        when(personalizationService.getInferredInterests(userId)).thenReturn(List.of(interest));

        mockMvc.perform(get("/api/v1/personalization/interests/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].topicId").value(5))
                .andExpect(jsonPath("$[0].topicName").value("Technology"))
                .andExpect(jsonPath("$[0].articlesRead").value(15))
                .andExpect(jsonPath("$[0].inferredInterestScore").value(0.75));
    }

    @Test
    void getInferredInterests_withNoInterests_shouldReturnEmptyList() throws Exception {
        Long userId = 1L;

        when(personalizationService.getInferredInterests(userId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/personalization/interests/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getSimilarArticles_shouldReturnSimilarArticlesList() throws Exception {
        Long userId = 1L;
        Long articleId = 101L;
        PersonalizedArticleDto similarArticle = PersonalizedArticleDto.builder()
                .articleId(102L)
                .title("Similar Article")
                .relevanceScore(0.78)
                .build();

        when(personalizationService.getSimilarArticles(userId, articleId, 10)).thenReturn(List.of(similarArticle));

        mockMvc.perform(get("/api/v1/personalization/similar/{userId}/{articleId}", userId, articleId)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].articleId").value(102))
                .andExpect(jsonPath("$[0].title").value("Similar Article"))
                .andExpect(jsonPath("$[0].relevanceScore").value(0.78));
    }

    @Test
    void getSimilarArticles_withDefaultLimit_shouldReturnArticles() throws Exception {
        Long userId = 1L;
        Long articleId = 101L;

        when(personalizationService.getSimilarArticles(userId, articleId, 10)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/personalization/similar/{userId}/{articleId}", userId, articleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getSimilarArticles_withCustomLimit_shouldReturnLimitedResults() throws Exception {
        Long userId = 1L;
        Long articleId = 101L;
        int customLimit = 5;

        when(personalizationService.getSimilarArticles(userId, articleId, customLimit)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/personalization/similar/{userId}/{articleId}", userId, articleId)
                        .param("limit", String.valueOf(customLimit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void handleRuntimeException_shouldReturnBadRequest() throws Exception {
        Long userId = 1L;
        String errorMessage = "Test runtime exception";

        when(personalizationService.getUserPreferences(userId)).thenThrow(new RuntimeException(errorMessage));

        mockMvc.perform(get("/api/v1/personalization/preferences/{userId}", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(errorMessage));
    }

    @Test
    void handleRuntimeException_onRecordInteraction_shouldReturnBadRequest() throws Exception {
        Long userId = 999L;
        String errorMessage = "User not found: 999";
        ArticleInteractionDto interaction = ArticleInteractionDto.builder()
                .articleId(101L)
                .timeSpentSeconds(180)
                .build();

        doNothing().when(personalizationService).recordReadingInteraction(eq(1L), any(ArticleInteractionDto.class));
        when(personalizationService.getUserPreferences(userId)).thenThrow(new RuntimeException(errorMessage));

        mockMvc.perform(get("/api/v1/personalization/preferences/{userId}", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(errorMessage));
    }
}