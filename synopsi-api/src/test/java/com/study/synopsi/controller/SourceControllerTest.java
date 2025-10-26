package com.study.synopsi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.synopsi.config.JwtAuthenticationFilter;
import com.study.synopsi.config.JwtUtil;
import com.study.synopsi.dto.SourceRequestDto;
import com.study.synopsi.dto.SourceResponseDto;
import com.study.synopsi.exception.GlobalExceptionHandler;
import com.study.synopsi.exception.SourceNotFoundException;
import com.study.synopsi.model.Source;
import com.study.synopsi.service.AuthService;
import com.study.synopsi.service.SourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SourceController.class,
        properties = "spring.main.allow-bean-definition-overriding=true")
@Import(GlobalExceptionHandler.class)
@DisplayName("SourceController Integration Tests")
@AutoConfigureMockMvc(addFilters = false)
class SourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SourceService sourceService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private AuthService authService;

    @MockBean
    private AuthenticationManager authenticationManager;

    private SourceRequestDto requestDto;
    private SourceResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = SourceRequestDto.builder()
                .name("TechCrunch")
                .baseUrl("https://techcrunch.com")
                .description("Leading technology news site")
                .credibilityScore(0.85)
                .language("en")
                .country("US")
                .sourceType(Source.SourceType.NEWS)
                .isActive(true)
                .build();

        responseDto = SourceResponseDto.builder()
                .id(1L)
                .name("TechCrunch")
                .baseUrl("https://techcrunch.com")
                .description("Leading technology news site")
                .credibilityScore(0.85)
                .language("en")
                .country("US")
                .sourceType("NEWS")
                .isActive(true)
                .feedCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/sources")
    class CreateSourceTests {

        @Test
        @DisplayName("Should create source successfully and return 201")
        void shouldCreateSourceSuccessfully() throws Exception {
            when(sourceService.createSource(any(SourceRequestDto.class))).thenReturn(responseDto);

            mockMvc.perform(post("/api/v1/sources")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("TechCrunch"))
                    .andExpect(jsonPath("$.baseUrl").value("https://techcrunch.com"))
                    .andExpect(jsonPath("$.credibilityScore").value(0.85))
                    .andExpect(jsonPath("$.sourceType").value("NEWS"));

            verify(sourceService).createSource(any(SourceRequestDto.class));
        }

        @Test
        @DisplayName("Should return 400 when name is missing")
        void shouldReturn400WhenNameIsMissing() throws Exception {
            requestDto.setName(null);

            mockMvc.perform(post("/api/v1/sources")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Validation Failed"))
                    .andExpect(jsonPath("$.fieldErrors.name").exists());

            verify(sourceService, never()).createSource(any());
        }

        @Test
        @DisplayName("Should return 400 when baseUrl is invalid")
        void shouldReturn400WhenBaseUrlIsInvalid() throws Exception {
            requestDto.setBaseUrl("invalid-url");

            mockMvc.perform(post("/api/v1/sources")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fieldErrors.baseUrl").exists());

            verify(sourceService, never()).createSource(any());
        }

        @Test
        @DisplayName("Should return 400 when credibility score is out of range")
        void shouldReturn400WhenCredibilityScoreOutOfRange() throws Exception {
            requestDto.setCredibilityScore(1.5);

            mockMvc.perform(post("/api/v1/sources")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fieldErrors.credibilityScore").exists());

            verify(sourceService, never()).createSource(any());
        }

        @Test
        @DisplayName("Should return 400 when source name already exists")
        void shouldReturn400WhenSourceNameExists() throws Exception {
            when(sourceService.createSource(any(SourceRequestDto.class)))
                    .thenThrow(new IllegalArgumentException("Source with name 'TechCrunch' already exists"));

            mockMvc.perform(post("/api/v1/sources")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Source with name 'TechCrunch' already exists"));

            verify(sourceService).createSource(any(SourceRequestDto.class));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/sources/{id}")
    class GetSourceByIdTests {

        @Test
        @DisplayName("Should get source by ID successfully")
        void shouldGetSourceByIdSuccessfully() throws Exception {
            when(sourceService.getSourceById(1L)).thenReturn(responseDto);

            mockMvc.perform(get("/api/v1/sources/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("TechCrunch"))
                    .andExpect(jsonPath("$.baseUrl").value("https://techcrunch.com"));

            verify(sourceService).getSourceById(1L);
        }

        @Test
        @DisplayName("Should return 404 when source not found")
        void shouldReturn404WhenSourceNotFound() throws Exception {
            when(sourceService.getSourceById(999L))
                    .thenThrow(new SourceNotFoundException(999L));

            mockMvc.perform(get("/api/v1/sources/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Source not found with id: 999"));

            verify(sourceService).getSourceById(999L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/sources/{id}/with-feeds")
    class GetSourceWithFeedsTests {

        @Test
        @DisplayName("Should get source with feeds successfully")
        void shouldGetSourceWithFeedsSuccessfully() throws Exception {
            responseDto.setFeedCount(3);
            when(sourceService.getSourceByIdWithFeeds(1L)).thenReturn(responseDto);

            mockMvc.perform(get("/api/v1/sources/1/with-feeds"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.feedCount").value(3));

            verify(sourceService).getSourceByIdWithFeeds(1L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/sources/by-name/{name}")
    class GetSourceByNameTests {

        @Test
        @DisplayName("Should get source by name successfully")
        void shouldGetSourceByNameSuccessfully() throws Exception {
            when(sourceService.getSourceByName("TechCrunch")).thenReturn(responseDto);

            mockMvc.perform(get("/api/v1/sources/by-name/TechCrunch"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("TechCrunch"));

            verify(sourceService).getSourceByName("TechCrunch");
        }

        @Test
        @DisplayName("Should return 404 when source name not found")
        void shouldReturn404WhenSourceNameNotFound() throws Exception {
            when(sourceService.getSourceByName("NonExistent"))
                    .thenThrow(new SourceNotFoundException("NonExistent"));

            mockMvc.perform(get("/api/v1/sources/by-name/NonExistent"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));

            verify(sourceService).getSourceByName("NonExistent");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/sources")
    class GetAllSourcesTests {

        @Test
        @DisplayName("Should get all sources successfully")
        void shouldGetAllSourcesSuccessfully() throws Exception {
            List<SourceResponseDto> sources = Arrays.asList(responseDto, responseDto);
            when(sourceService.getAllSources()).thenReturn(sources);

            mockMvc.perform(get("/api/v1/sources"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name").value("TechCrunch"));

            verify(sourceService).getAllSources();
        }

        @Test
        @DisplayName("Should get active sources when active=true")
        void shouldGetActiveSourcesWhenActiveTrue() throws Exception {
            List<SourceResponseDto> sources = Arrays.asList(responseDto);
            when(sourceService.getActiveSources()).thenReturn(sources);

            mockMvc.perform(get("/api/v1/sources?active=true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(sourceService).getActiveSources();
            verify(sourceService, never()).getAllSources();
        }

        @Test
        @DisplayName("Should get sources by type when type parameter provided")
        void shouldGetSourcesByTypeWhenTypeProvided() throws Exception {
            List<SourceResponseDto> sources = Arrays.asList(responseDto);
            when(sourceService.getSourcesByType(Source.SourceType.NEWS)).thenReturn(sources);

            mockMvc.perform(get("/api/v1/sources?type=NEWS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(sourceService).getSourcesByType(Source.SourceType.NEWS);
        }

        @Test
        @DisplayName("Should get sources with feeds when includeFeeds=true")
        void shouldGetSourcesWithFeedsWhenIncludeFeedsTrue() throws Exception {
            List<SourceResponseDto> sources = Arrays.asList(responseDto);
            when(sourceService.getAllSourcesWithFeeds()).thenReturn(sources);

            mockMvc.perform(get("/api/v1/sources?includeFeeds=true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(sourceService).getAllSourcesWithFeeds();
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/sources/{id}")
    class UpdateSourceTests {

        @Test
        @DisplayName("Should update source successfully")
        void shouldUpdateSourceSuccessfully() throws Exception {
            when(sourceService.updateSource(eq(1L), any(SourceRequestDto.class))).thenReturn(responseDto);

            mockMvc.perform(put("/api/v1/sources/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("TechCrunch"));

            verify(sourceService).updateSource(eq(1L), any(SourceRequestDto.class));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent source")
        void shouldReturn404WhenUpdatingNonExistentSource() throws Exception {
            when(sourceService.updateSource(eq(999L), any(SourceRequestDto.class)))
                    .thenThrow(new SourceNotFoundException(999L));

            mockMvc.perform(put("/api/v1/sources/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));

            verify(sourceService).updateSource(eq(999L), any(SourceRequestDto.class));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/sources/{id}")
    class PartialUpdateSourceTests {

        @Test
        @DisplayName("Should partially update source successfully")
        void shouldPartiallyUpdateSourceSuccessfully() throws Exception {
            when(sourceService.updateSource(eq(1L), any(SourceRequestDto.class))).thenReturn(responseDto);

            mockMvc.perform(patch("/api/v1/sources/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(sourceService).updateSource(eq(1L), any(SourceRequestDto.class));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/sources/{id}/activate")
    class ActivateSourceTests {

        @Test
        @DisplayName("Should activate source successfully")
        void shouldActivateSourceSuccessfully() throws Exception {
            when(sourceService.activateSource(1L)).thenReturn(responseDto);

            mockMvc.perform(patch("/api/v1/sources/1/activate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.isActive").value(true));

            verify(sourceService).activateSource(1L);
        }

        @Test
        @DisplayName("Should return 404 when activating non-existent source")
        void shouldReturn404WhenActivatingNonExistentSource() throws Exception {
            when(sourceService.activateSource(999L))
                    .thenThrow(new SourceNotFoundException(999L));

            mockMvc.perform(patch("/api/v1/sources/999/activate"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));

            verify(sourceService).activateSource(999L);
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/sources/{id}/deactivate")
    class DeactivateSourceTests {

        @Test
        @DisplayName("Should deactivate source successfully")
        void shouldDeactivateSourceSuccessfully() throws Exception {
            responseDto.setIsActive(false);
            when(sourceService.deactivateSource(1L)).thenReturn(responseDto);

            mockMvc.perform(patch("/api/v1/sources/1/deactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.isActive").value(false));

            verify(sourceService).deactivateSource(1L);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/sources/{id}")
    class DeleteSourceTests {

        @Test
        @DisplayName("Should delete source successfully and return 204")
        void shouldDeleteSourceSuccessfully() throws Exception {
            doNothing().when(sourceService).deleteSource(1L);

            mockMvc.perform(delete("/api/v1/sources/1"))
                    .andExpect(status().isNoContent());

            verify(sourceService).deleteSource(1L);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent source")
        void shouldReturn404WhenDeletingNonExistentSource() throws Exception {
            doThrow(new SourceNotFoundException(999L)).when(sourceService).deleteSource(999L);

            mockMvc.perform(delete("/api/v1/sources/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));

            verify(sourceService).deleteSource(999L);
        }
    }
}