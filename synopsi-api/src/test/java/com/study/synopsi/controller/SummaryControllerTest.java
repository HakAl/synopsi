package com.study.synopsi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.synopsi.config.JwtAuthenticationFilter;
import com.study.synopsi.config.JwtUtil;
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
        SummaryJob job = new SummaryJob();
        job.setId(1L);

        when(summaryService.requestSummary(anyLong(), any(), any(Summary.SummaryType.class), any(Summary.SummaryLength.class)))
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
        Summary summary = new Summary();
        summary.setId(1L);

        when(summaryService.getSummary(anyLong(), any(), any(Summary.SummaryType.class)))
                .thenReturn(Optional.of(summary));

        mockMvc.perform(get("/api/v1/summaries/article/1")
                        .param("userId", "1")
                        .param("summaryType", "BRIEF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getSummary_shouldReturnNotFoundWhenMissing() throws Exception {
        when(summaryService.getSummary(anyLong(), any(), any(Summary.SummaryType.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/summaries/article/1")
                        .param("userId", "1")
                        .param("summaryType", "BRIEF"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void getDefaultSummary_shouldReturnSummary() throws Exception {
        Summary summary = new Summary();
        summary.setId(1L);

        when(summaryService.getDefaultSummary(1L, Summary.SummaryType.BRIEF)).thenReturn(Optional.of(summary));

        mockMvc.perform(get("/api/v1/summaries/article/1/default")
                        .param("summaryType", "BRIEF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getSummaryById_shouldReturnSummary() throws Exception {
        Summary summary = new Summary();
        summary.setId(1L);

        when(summaryService.getSummaryById(1L)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/summaries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getArticleSummaries_shouldReturnListOfSummaries() throws Exception {
        Summary summary = new Summary();
        summary.setId(1L);
        List<Summary> summaries = Collections.singletonList(summary);

        when(summaryService.getArticleSummaries(1L)).thenReturn(summaries);

        mockMvc.perform(get("/api/v1/summaries/article/1/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void getUserSummaries_shouldReturnPageOfSummaries() throws Exception {
        Summary summary = new Summary();
        summary.setId(1L);
        Page<Summary> pagedSummaries = new PageImpl<>(Collections.singletonList(summary));

        when(summaryService.getUserSummaries(anyLong(), any(PageRequest.class)))
                .thenReturn(pagedSummaries);

        mockMvc.perform(get("/api/v1/summaries/user/1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    void regenerateSummary_shouldReturnAccepted() throws Exception {
        SummaryJob job = new SummaryJob();
        job.setId(2L);

        when(summaryService.regenerateSummary(1L)).thenReturn(job);

        mockMvc.perform(post("/api/v1/summaries/1/regenerate"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(2L));
    }

    @Test
    void getJobById_shouldReturnJob() throws Exception {
        SummaryJob job = new SummaryJob();
        job.setId(1L);

        when(summaryService.getJobById(1L)).thenReturn(job);

        mockMvc.perform(get("/api/v1/summaries/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getQueuedJobs_shouldReturnListOfJobs() throws Exception {
        SummaryJob job = new SummaryJob();
        job.setId(1L);
        List<SummaryJob> jobs = Collections.singletonList(job);

        when(summaryService.getQueuedJobs()).thenReturn(jobs);

        mockMvc.perform(get("/api/v1/summaries/jobs/queued"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void retryFailedJob_shouldReturnOk() throws Exception {
        SummaryJob job = new SummaryJob();
        job.setId(1L);

        when(summaryService.retryFailedJob(1L)).thenReturn(job);

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