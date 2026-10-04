package com.study.synopsi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.synopsi.config.JwtAuthenticationFilter;
import com.study.synopsi.config.JwtUtil;
import com.study.synopsi.dto.SummaryJobResponseDto;
import com.study.synopsi.dto.SummaryResponseDto;
import com.study.synopsi.model.Summary;
import com.study.synopsi.model.SummaryJob;
import com.study.synopsi.service.AuthService;
import com.study.synopsi.service.SummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SummaryController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SummaryControllerTest {

    // These are @WebMvcTest with a mocked service, so they assert routing,
    // status codes and JSON shape only. They cannot catch a serialization
    // failure on a real entity, which is why returning lazy JPA entities from
    // this controller went unnoticed. SummaryJobSerializationTest covers that.

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SummaryService summaryService;

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
    void requestSummary_shouldReturnAccepted() throws Exception {
        SummaryJobResponseDto job = new SummaryJobResponseDto();
        job.setId(1L);

        when(summaryService.requestSummaryAsDto(anyLong(), any(), any(Summary.SummaryType.class), any(Summary.SummaryLength.class)))
                .thenReturn(job);

        mockMvc.perform(post("/api/v1/summaries/request")
                        .param("articleId", "1")
                        .param("userId", "1")
                        .param("summaryType", "BRIEF")
                        .param("summaryLength", "MEDIUM"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getSummary_shouldReturnSummaryWhenFound() throws Exception {
        SummaryResponseDto summary = new SummaryResponseDto();
        summary.setId(1L);

        when(summaryService.getSummaryAsDto(anyLong(), any(), any(Summary.SummaryType.class)))
                .thenReturn(Optional.of(summary));

        mockMvc.perform(get("/api/v1/summaries/article/1")
                        .param("userId", "1")
                        .param("summaryType", "BRIEF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getSummary_shouldReturnNotFoundWhenMissing() throws Exception {
        when(summaryService.getSummaryAsDto(anyLong(), any(), any(Summary.SummaryType.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/summaries/article/1")
                        .param("userId", "1")
                        .param("summaryType", "BRIEF"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void getDefaultSummary_shouldReturnSummary() throws Exception {
        SummaryResponseDto summary = new SummaryResponseDto();
        summary.setId(1L);

        when(summaryService.getDefaultSummaryAsDto(1L, Summary.SummaryType.BRIEF)).thenReturn(Optional.of(summary));

        mockMvc.perform(get("/api/v1/summaries/article/1/default")
                        .param("summaryType", "BRIEF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getSummaryById_shouldReturnSummary() throws Exception {
        SummaryResponseDto summary = new SummaryResponseDto();
        summary.setId(1L);

        when(summaryService.getSummaryByIdAsDto(1L)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/summaries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getArticleSummaries_shouldReturnListOfSummaries() throws Exception {
        SummaryResponseDto summary = new SummaryResponseDto();
        summary.setId(1L);
        List<SummaryResponseDto> summaries = Collections.singletonList(summary);

        when(summaryService.getArticleSummariesAsDtos(1L)).thenReturn(summaries);

        mockMvc.perform(get("/api/v1/summaries/article/1/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void getUserSummaries_shouldReturnPageOfSummaries() throws Exception {
        SummaryResponseDto summary = new SummaryResponseDto();
        summary.setId(1L);
        Page<SummaryResponseDto> pagedSummaries = new PageImpl<>(Collections.singletonList(summary));

        when(summaryService.getUserSummariesAsDtos(anyLong(), any(PageRequest.class)))
                .thenReturn(pagedSummaries);

        mockMvc.perform(get("/api/v1/summaries/user/1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    void regenerateSummary_shouldReturnAccepted() throws Exception {
        SummaryJobResponseDto job = new SummaryJobResponseDto();
        job.setId(2L);

        when(summaryService.regenerateSummaryAsDto(1L)).thenReturn(job);

        mockMvc.perform(post("/api/v1/summaries/1/regenerate"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(2L));
    }

    @Test
    void getJobById_shouldReturnJob() throws Exception {
        SummaryJobResponseDto job = new SummaryJobResponseDto();
        job.setId(1L);

        when(summaryService.getJobByIdAsDto(1L)).thenReturn(job);

        mockMvc.perform(get("/api/v1/summaries/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getQueuedJobs_shouldReturnListOfJobs() throws Exception {
        SummaryJobResponseDto job = new SummaryJobResponseDto();
        job.setId(1L);
        List<SummaryJobResponseDto> jobs = Collections.singletonList(job);

        when(summaryService.getQueuedJobsAsDtos()).thenReturn(jobs);

        mockMvc.perform(get("/api/v1/summaries/jobs/queued"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void retryFailedJob_shouldReturnOk() throws Exception {
        SummaryJobResponseDto job = new SummaryJobResponseDto();
        job.setId(1L);

        when(summaryService.retryFailedJobAsDto(1L)).thenReturn(job);

        mockMvc.perform(post("/api/v1/summaries/jobs/1/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void summaryExists_shouldReturnBoolean() throws Exception {
        when(summaryService.summaryExists(1L, 1L, Summary.SummaryType.BRIEF)).thenReturn(true);

        mockMvc.perform(get("/api/v1/summaries/exists")
                        .param("articleId", "1")
                        .param("userId", "1")
                        .param("summaryType", "BRIEF"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void handleWorkerCallback_shouldReturnOk() throws Exception {
        doNothing().when(summaryService).handleWorkerCallback(anyLong(), any(), any(), any());

        mockMvc.perform(post("/api/v1/summaries/callback/complete")
                        .param("jobId", "1")
                        .param("summaryText", "This is a summary.")
                        .param("modelVersion", "v1")
                        .param("tokenCount", "100"))
                .andExpect(status().isOk());
    }

    @Test
    void handleWorkerFailure_shouldReturnOk() throws Exception {
        doNothing().when(summaryService).handleWorkerFailure(anyLong(), any());

        mockMvc.perform(post("/api/v1/summaries/callback/failure")
                        .param("jobId", "1")
                        .param("errorMessage", "An error occurred."))
                .andExpect(status().isOk());
    }
}