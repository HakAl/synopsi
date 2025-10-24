package com.study.synopsi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.synopsi.dto.TopicRequestDto;
import com.study.synopsi.dto.TopicResponseDto;
import com.study.synopsi.exception.GlobalExceptionHandler;
import com.study.synopsi.exception.InvalidTopicHierarchyException;
import com.study.synopsi.exception.TopicNotFoundException;
import com.study.synopsi.service.TopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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

@WebMvcTest(controllers = TopicController.class,
        properties = "spring.main.allow-bean-definition-overriding=true")
@Import(GlobalExceptionHandler.class)
@DisplayName("TopicController Integration Tests")
class TopicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TopicService topicService;

    private TopicRequestDto requestDto;
    private TopicResponseDto rootTopicResponseDto;
    private TopicResponseDto childTopicResponseDto;

    @BeforeEach
    void setUp() {
        requestDto = TopicRequestDto.builder()
                .name("Technology")
                .slug("technology")
                .description("All about technology")
                .isActive(true)
                .build();

        rootTopicResponseDto = TopicResponseDto.builder()
                .id(1L)
                .name("Technology")
                .slug("technology")
                .description("All about technology")
                .hierarchyPath(Arrays.asList("Technology"))
                .depth(0)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .childTopicCount(0)
                .build();

        childTopicResponseDto = TopicResponseDto.builder()
                .id(2L)
                .name("Artificial Intelligence")
                .slug("artificial-intelligence")
                .description("AI and Machine Learning")
                .parentTopicId(1L)
                .parentTopicName("Technology")
                .hierarchyPath(Arrays.asList("Technology", "Artificial Intelligence"))
                .depth(1)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .childTopicCount(0)
                .build();
    }

    @Nested
    @DisplayName("POST /api/topics")
    class CreateTopicTests {

        @Test
        @DisplayName("Should create root topic successfully and return 201")
        void shouldCreateRootTopicSuccessfully() throws Exception {
            when(topicService.createTopic(any(TopicRequestDto.class))).thenReturn(rootTopicResponseDto);

            mockMvc.perform(post("/api/topics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Technology"))
                    .andExpect(jsonPath("$.slug").value("technology"))
                    .andExpect(jsonPath("$.depth").value(0))
                    .andExpect(jsonPath("$.hierarchyPath", hasSize(1)))
                    .andExpect(jsonPath("$.hierarchyPath[0]").value("Technology"));

            verify(topicService).createTopic(any(TopicRequestDto.class));
        }

        @Test
        @DisplayName("Should create child topic successfully")
        void shouldCreateChildTopicSuccessfully() throws Exception {
            TopicRequestDto childRequest = TopicRequestDto.builder()
                    .name("Artificial Intelligence")
                    .slug("artificial-intelligence")
                    .parentTopicId(1L)
                    .build();

            when(topicService.createTopic(any(TopicRequestDto.class))).thenReturn(childTopicResponseDto);

            mockMvc.perform(post("/api/topics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(childRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.parentTopicId").value(1))
                    .andExpect(jsonPath("$.depth").value(1))
                    .andExpect(jsonPath("$.hierarchyPath", hasSize(2)));

            verify(topicService).createTopic(any(TopicRequestDto.class));
        }

        @Test
        @DisplayName("Should return 400 when name is missing")
        void shouldReturn400WhenNameIsMissing() throws Exception {
            requestDto.setName(null);

            mockMvc.perform(post("/api/topics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Validation Failed"))
                    .andExpect(jsonPath("$.fieldErrors.name").exists());

            verify(topicService, never()).createTopic(any());
        }

        @Test
        @DisplayName("Should return 400 when slug is invalid")
        void shouldReturn400WhenSlugIsInvalid() throws Exception {
            requestDto.setSlug("Invalid Slug!");

            mockMvc.perform(post("/api/topics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fieldErrors.slug").exists());

            verify(topicService, never()).createTopic(any());
        }

        @Test
        @DisplayName("Should return 400 when topic name already exists")
        void shouldReturn400WhenTopicNameExists() throws Exception {
            when(topicService.createTopic(any(TopicRequestDto.class)))
                    .thenThrow(new IllegalArgumentException("Topic with name 'Technology' already exists"));

            mockMvc.perform(post("/api/topics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Topic with name 'Technology' already exists"));

            verify(topicService).createTopic(any(TopicRequestDto.class));
        }

        @Test
        @DisplayName("Should return 404 when parent topic not found")
        void shouldReturn404WhenParentTopicNotFound() throws Exception {
            requestDto.setParentTopicId(999L);
            when(topicService.createTopic(any(TopicRequestDto.class)))
                    .thenThrow(new TopicNotFoundException(999L));

            mockMvc.perform(post("/api/topics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));

            verify(topicService).createTopic(any(TopicRequestDto.class));
        }

        @Test
        @DisplayName("Should return 400 when hierarchy depth exceeded")
        void shouldReturn400WhenHierarchyDepthExceeded() throws Exception {
            requestDto.setParentTopicId(1L);
            when(topicService.createTopic(any(TopicRequestDto.class)))
                    .thenThrow(InvalidTopicHierarchyException.maxDepthExceeded(4));

            mockMvc.perform(post("/api/topics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message", containsString("exceeds maximum depth")));

            verify(topicService).createTopic(any(TopicRequestDto.class));
        }
    }

    @Nested
    @DisplayName("GET /api/topics/{id}")
    class GetTopicByIdTests {

        @Test
        @DisplayName("Should get topic by ID successfully")
        void shouldGetTopicByIdSuccessfully() throws Exception {
            when(topicService.getTopicById(1L)).thenReturn(rootTopicResponseDto);

            mockMvc.perform(get("/api/topics/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Technology"))
                    .andExpect(jsonPath("$.slug").value("technology"));

            verify(topicService).getTopicById(1L);
        }

        @Test
        @DisplayName("Should get topic with children when includeChildren=true")
        void shouldGetTopicWithChildrenWhenIncludeChildrenTrue() throws Exception {
            when(topicService.getTopicByIdWithChildren(1L)).thenReturn(rootTopicResponseDto);

            mockMvc.perform(get("/api/topics/1?includeChildren=true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(topicService).getTopicByIdWithChildren(1L);
            verify(topicService, never()).getTopicById(anyLong());
        }

        @Test
        @DisplayName("Should return 404 when topic not found")
        void shouldReturn404WhenTopicNotFound() throws Exception {
            when(topicService.getTopicById(999L))
                    .thenThrow(new TopicNotFoundException(999L));

            mockMvc.perform(get("/api/topics/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Topic not found with id: 999"));

            verify(topicService).getTopicById(999L);
        }
    }

    @Nested
    @DisplayName("GET /api/topics/by-slug/{slug}")
    class GetTopicBySlugTests {

        @Test
        @DisplayName("Should get topic by slug successfully")
        void shouldGetTopicBySlugSuccessfully() throws Exception {
            when(topicService.getTopicBySlug("technology")).thenReturn(rootTopicResponseDto);

            mockMvc.perform(get("/api/topics/by-slug/technology"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slug").value("technology"))
                    .andExpect(jsonPath("$.name").value("Technology"));

            verify(topicService).getTopicBySlug("technology");
        }

        @Test
        @DisplayName("Should return 404 when topic slug not found")
        void shouldReturn404WhenTopicSlugNotFound() throws Exception {
            when(topicService.getTopicBySlug("non-existent"))
                    .thenThrow(new TopicNotFoundException("non-existent"));

            mockMvc.perform(get("/api/topics/by-slug/non-existent"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));

            verify(topicService).getTopicBySlug("non-existent");
        }
    }

    @Nested
    @DisplayName("GET /api/topics")
    class GetAllTopicsTests {

        @Test
        @DisplayName("Should get all topics successfully")
        void shouldGetAllTopicsSuccessfully() throws Exception {
            List<TopicResponseDto> topics = Arrays.asList(rootTopicResponseDto, childTopicResponseDto);
            when(topicService.getAllTopics()).thenReturn(topics);

            mockMvc.perform(get("/api/topics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name").value("Technology"))
                    .andExpect(jsonPath("$[1].name").value("Artificial Intelligence"));

            verify(topicService).getAllTopics();
        }

        @Test
        @DisplayName("Should get active topics when active=true")
        void shouldGetActiveTopicsWhenActiveTrue() throws Exception {
            List<TopicResponseDto> topics = Arrays.asList(rootTopicResponseDto);
            when(topicService.getActiveTopics()).thenReturn(topics);

            mockMvc.perform(get("/api/topics?active=true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(topicService).getActiveTopics();
            verify(topicService, never()).getAllTopics();
        }

        @Test
        @DisplayName("Should get root topics when rootOnly=true")
        void shouldGetRootTopicsWhenRootOnlyTrue() throws Exception {
            List<TopicResponseDto> topics = Arrays.asList(rootTopicResponseDto);
            when(topicService.getRootTopics()).thenReturn(topics);

            mockMvc.perform(get("/api/topics?rootOnly=true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].depth").value(0));

            verify(topicService).getRootTopics();
            verify(topicService, never()).getAllTopics();
        }
    }

    @Nested
    @DisplayName("GET /api/topics/root")
    class GetRootTopicsTests {

        @Test
        @DisplayName("Should get root topics successfully")
        void shouldGetRootTopicsSuccessfully() throws Exception {
            List<TopicResponseDto> topics = Arrays.asList(rootTopicResponseDto);
            when(topicService.getRootTopics()).thenReturn(topics);

            mockMvc.perform(get("/api/topics/root"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].depth").value(0));

            verify(topicService).getRootTopics();
        }

        @Test
        @DisplayName("Should get root topics with children when includeChildren=true")
        void shouldGetRootTopicsWithChildrenWhenIncludeChildrenTrue() throws Exception {
            List<TopicResponseDto> topics = Arrays.asList(rootTopicResponseDto);
            when(topicService.getRootTopicsWithChildren()).thenReturn(topics);

            mockMvc.perform(get("/api/topics/root?includeChildren=true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(topicService).getRootTopicsWithChildren();
            verify(topicService, never()).getRootTopics();
        }
    }

    @Nested
    @DisplayName("GET /api/topics/{parentId}/children")
    class GetChildTopicsTests {

        @Test
        @DisplayName("Should get child topics successfully")
        void shouldGetChildTopicsSuccessfully() throws Exception {
            List<TopicResponseDto> childTopics = Arrays.asList(childTopicResponseDto);
            when(topicService.getChildTopics(1L)).thenReturn(childTopics);

            mockMvc.perform(get("/api/topics/1/children"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].parentTopicId").value(1))
                    .andExpect(jsonPath("$[0].depth").value(1));

            verify(topicService).getChildTopics(1L);
        }

        @Test
        @DisplayName("Should return 404 when parent topic not found")
        void shouldReturn404WhenParentTopicNotFound() throws Exception {
            when(topicService.getChildTopics(999L))
                    .thenThrow(new TopicNotFoundException(999L));

            mockMvc.perform(get("/api/topics/999/children"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));

            verify(topicService).getChildTopics(999L);
        }
    }

    @Nested
    @DisplayName("PUT /api/topics/{id}")
    class UpdateTopicTests {

        @Test
        @DisplayName("Should update topic successfully")
        void shouldUpdateTopicSuccessfully() throws Exception {
            when(topicService.updateTopic(eq(1L), any(TopicRequestDto.class))).thenReturn(rootTopicResponseDto);

            mockMvc.perform(put("/api/topics/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Technology"));

            verify(topicService).updateTopic(eq(1L), any(TopicRequestDto.class));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent topic")
        void shouldReturn404WhenUpdatingNonExistentTopic() throws Exception {
            when(topicService.updateTopic(eq(999L), any(TopicRequestDto.class)))
                    .thenThrow(new TopicNotFoundException(999L));

            mockMvc.perform(put("/api/topics/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));

            verify(topicService).updateTopic(eq(999L), any(TopicRequestDto.class));
        }

        @Test
        @DisplayName("Should return 400 when creating circular reference")
        void shouldReturn400WhenCreatingCircularReference() throws Exception {
            requestDto.setParentTopicId(2L);
            when(topicService.updateTopic(eq(1L), any(TopicRequestDto.class)))
                    .thenThrow(InvalidTopicHierarchyException.circularReference(1L, 2L));

            mockMvc.perform(put("/api/topics/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message", containsString("Circular reference")));

            verify(topicService).updateTopic(eq(1L), any(TopicRequestDto.class));
        }
    }

    @Nested
    @DisplayName("PATCH /api/topics/{id}")
    class PartialUpdateTopicTests {

        @Test
        @DisplayName("Should partially update topic successfully")
        void shouldPartiallyUpdateTopicSuccessfully() throws Exception {
            when(topicService.updateTopic(eq(1L), any(TopicRequestDto.class))).thenReturn(rootTopicResponseDto);

            mockMvc.perform(patch("/api/topics/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(topicService).updateTopic(eq(1L), any(TopicRequestDto.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/topics/{id}")
    class DeleteTopicTests {

        @Test
        @DisplayName("Should delete topic successfully and return 204")
        void shouldDeleteTopicSuccessfully() throws Exception {
            doNothing().when(topicService).deleteTopic(1L);

            mockMvc.perform(delete("/api/topics/1"))
                    .andExpect(status().isNoContent());

            verify(topicService).deleteTopic(1L);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent topic")
        void shouldReturn404WhenDeletingNonExistentTopic() throws Exception {
            doThrow(new TopicNotFoundException(999L)).when(topicService).deleteTopic(999L);

            mockMvc.perform(delete("/api/topics/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));

            verify(topicService).deleteTopic(999L);
        }

        @Test
        @DisplayName("Should return 400 when deleting topic with children")
        void shouldReturn400WhenDeletingTopicWithChildren() throws Exception {
            doThrow(new IllegalArgumentException("Cannot delete topic with id 1 because it has 2 child topics"))
                    .when(topicService).deleteTopic(1L);

            mockMvc.perform(delete("/api/topics/1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message", containsString("Cannot delete topic")));

            verify(topicService).deleteTopic(1L);
        }
    }
}