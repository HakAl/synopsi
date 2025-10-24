package com.study.synopsi.service;

import com.study.synopsi.dto.TopicRequestDto;
import com.study.synopsi.dto.TopicResponseDto;
import com.study.synopsi.exception.InvalidTopicHierarchyException;
import com.study.synopsi.exception.TopicNotFoundException;
import com.study.synopsi.mapper.TopicMapper;
import com.study.synopsi.model.Topic;
import com.study.synopsi.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TopicService Tests")
class TopicServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private TopicMapper topicMapper;

    @InjectMocks
    private TopicService topicService;

    private Topic testRootTopic;
    private Topic testChildTopic;
    private Topic testGrandchildTopic;
    private TopicRequestDto testRequestDto;
    private TopicResponseDto testResponseDto;

    @BeforeEach
    void setUp() {
        // Setup root topic
        testRootTopic = new Topic();
        testRootTopic.setId(1L);
        testRootTopic.setName("Technology");
        testRootTopic.setSlug("technology");
        testRootTopic.setDescription("All about technology");
        testRootTopic.setParentTopic(null);
        testRootTopic.setIsActive(true);
        testRootTopic.setCreatedAt(LocalDateTime.now());
        testRootTopic.setUpdatedAt(LocalDateTime.now());
        testRootTopic.setChildTopics(new HashSet<>());

        // Setup child topic
        testChildTopic = new Topic();
        testChildTopic.setId(2L);
        testChildTopic.setName("Artificial Intelligence");
        testChildTopic.setSlug("artificial-intelligence");
        testChildTopic.setDescription("AI and Machine Learning");
        testChildTopic.setParentTopic(testRootTopic);
        testChildTopic.setIsActive(true);
        testChildTopic.setCreatedAt(LocalDateTime.now());
        testChildTopic.setUpdatedAt(LocalDateTime.now());
        testChildTopic.setChildTopics(new HashSet<>());

        // Setup grandchild topic
        testGrandchildTopic = new Topic();
        testGrandchildTopic.setId(3L);
        testGrandchildTopic.setName("Natural Language Processing");
        testGrandchildTopic.setSlug("nlp");
        testGrandchildTopic.setDescription("NLP and text processing");
        testGrandchildTopic.setParentTopic(testChildTopic);
        testGrandchildTopic.setIsActive(true);
        testGrandchildTopic.setCreatedAt(LocalDateTime.now());
        testGrandchildTopic.setUpdatedAt(LocalDateTime.now());
        testGrandchildTopic.setChildTopics(new HashSet<>());

        // Setup request DTO
        testRequestDto = TopicRequestDto.builder()
                .name("Technology")
                .slug("technology")
                .description("All about technology")
                .isActive(true)
                .build();

        // Setup response DTO
        testResponseDto = TopicResponseDto.builder()
                .id(1L)
                .name("Technology")
                .slug("technology")
                .description("All about technology")
                .hierarchyPath(Arrays.asList("Technology"))
                .depth(0)
                .isActive(true)
                .createdAt(testRootTopic.getCreatedAt())
                .updatedAt(testRootTopic.getUpdatedAt())
                .childTopicCount(0)
                .build();
    }

    @Nested
    @DisplayName("Create Topic Tests")
    class CreateTopicTests {

        @Test
        @DisplayName("Should create root topic successfully")
        void shouldCreateRootTopicSuccessfully() {
            // Given
            when(topicRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(topicRepository.existsBySlug(anyString())).thenReturn(false);
            when(topicMapper.toEntity(any(TopicRequestDto.class))).thenReturn(testRootTopic);
            when(topicRepository.save(any(Topic.class))).thenReturn(testRootTopic);
            when(topicMapper.toDto(any(Topic.class))).thenReturn(testResponseDto);

            // When
            TopicResponseDto result = topicService.createTopic(testRequestDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Technology");
            assertThat(result.getDepth()).isEqualTo(0);

            verify(topicRepository).existsByNameIgnoreCase("Technology");
            verify(topicRepository).existsBySlug("technology");
            verify(topicRepository).save(any(Topic.class));
        }

        @Test
        @DisplayName("Should create child topic successfully")
        void shouldCreateChildTopicSuccessfully() {
            // Given
            testRequestDto.setName("Artificial Intelligence");
            testRequestDto.setSlug("artificial-intelligence");
            testRequestDto.setParentTopicId(1L);

            when(topicRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(topicRepository.existsBySlug(anyString())).thenReturn(false);
            when(topicRepository.findById(1L)).thenReturn(Optional.of(testRootTopic));
            when(topicMapper.toEntity(any(TopicRequestDto.class))).thenReturn(testChildTopic);
            when(topicRepository.save(any(Topic.class))).thenReturn(testChildTopic);
            when(topicMapper.toDto(any(Topic.class))).thenReturn(testResponseDto);

            // When
            TopicResponseDto result = topicService.createTopic(testRequestDto);

            // Then
            assertThat(result).isNotNull();
            verify(topicRepository).findById(1L);
            verify(topicRepository).save(any(Topic.class));
        }

        @Test
        @DisplayName("Should throw exception when name already exists")
        void shouldThrowExceptionWhenNameExists() {
            // Given
            when(topicRepository.existsByNameIgnoreCase(anyString())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> topicService.createTopic(testRequestDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Topic with name 'Technology' already exists");

            verify(topicRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when slug already exists")
        void shouldThrowExceptionWhenSlugExists() {
            // Given
            when(topicRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(topicRepository.existsBySlug(anyString())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> topicService.createTopic(testRequestDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Topic with slug 'technology' already exists");

            verify(topicRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when parent topic not found")
        void shouldThrowExceptionWhenParentTopicNotFound() {
            // Given
            testRequestDto.setParentTopicId(999L);
            when(topicRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(topicRepository.existsBySlug(anyString())).thenReturn(false);
            when(topicRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> topicService.createTopic(testRequestDto))
                    .isInstanceOf(TopicNotFoundException.class)
                    .hasMessageContaining("Topic not found with id: 999");

            verify(topicRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when hierarchy depth exceeds maximum")
        void shouldThrowExceptionWhenHierarchyDepthExceeded() {
            // Given - Create a deep hierarchy (depth 3)
            Topic depth1 = new Topic();
            depth1.setId(1L);
            depth1.setParentTopic(null);

            Topic depth2 = new Topic();
            depth2.setId(2L);
            depth2.setParentTopic(depth1);

            Topic depth3 = new Topic();
            depth3.setId(3L);
            depth3.setParentTopic(depth2);

            Topic depth4 = new Topic();
            depth4.setId(4L);
            depth4.setParentTopic(depth3);

            testRequestDto.setParentTopicId(4L);
            when(topicRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(topicRepository.existsBySlug(anyString())).thenReturn(false);
            when(topicRepository.findById(4L)).thenReturn(Optional.of(depth4));

            // When/Then
            assertThatThrownBy(() -> topicService.createTopic(testRequestDto))
                    .isInstanceOf(InvalidTopicHierarchyException.class)
                    .hasMessageContaining("exceeds maximum depth of 4");

            verify(topicRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Topic Tests")
    class GetTopicTests {

        @Test
        @DisplayName("Should get topic by ID successfully")
        void shouldGetTopicByIdSuccessfully() {
            // Given
            when(topicRepository.findByIdWithParent(1L)).thenReturn(Optional.of(testRootTopic));
            when(topicMapper.toDto(any(Topic.class))).thenReturn(testResponseDto);

            // When
            TopicResponseDto result = topicService.getTopicById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Technology");

            verify(topicRepository).findByIdWithParent(1L);
            verify(topicMapper).toDto(testRootTopic);
        }

        @Test
        @DisplayName("Should throw exception when topic not found by ID")
        void shouldThrowExceptionWhenTopicNotFoundById() {
            // Given
            when(topicRepository.findByIdWithParent(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> topicService.getTopicById(999L))
                    .isInstanceOf(TopicNotFoundException.class)
                    .hasMessageContaining("Topic not found with id: 999");

            verify(topicRepository).findByIdWithParent(999L);
            verify(topicMapper, never()).toDto(any());
        }

        @Test
        @DisplayName("Should get topic by slug successfully")
        void shouldGetTopicBySlugSuccessfully() {
            // Given
            when(topicRepository.findBySlugWithParent("technology")).thenReturn(Optional.of(testRootTopic));
            when(topicMapper.toDto(any(Topic.class))).thenReturn(testResponseDto);

            // When
            TopicResponseDto result = topicService.getTopicBySlug("technology");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSlug()).isEqualTo("technology");

            verify(topicRepository).findBySlugWithParent("technology");
            verify(topicMapper).toDto(testRootTopic);
        }

        @Test
        @DisplayName("Should get topic with children successfully")
        void shouldGetTopicWithChildrenSuccessfully() {
            // Given
            when(topicRepository.findByIdWithChildren(1L)).thenReturn(Optional.of(testRootTopic));
            when(topicMapper.toDto(any(Topic.class))).thenReturn(testResponseDto);

            // When
            TopicResponseDto result = topicService.getTopicByIdWithChildren(1L);

            // Then
            assertThat(result).isNotNull();
            verify(topicRepository).findByIdWithChildren(1L);
            verify(topicMapper).toDto(testRootTopic);
        }

        @Test
        @DisplayName("Should get all topics successfully")
        void shouldGetAllTopicsSuccessfully() {
            // Given
            List<Topic> topics = Arrays.asList(testRootTopic, testChildTopic);
            List<TopicResponseDto> responseDtos = Arrays.asList(testResponseDto, testResponseDto);

            when(topicRepository.findAll()).thenReturn(topics);
            when(topicMapper.toDtoList(anyList())).thenReturn(responseDtos);

            // When
            List<TopicResponseDto> result = topicService.getAllTopics();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);

            verify(topicRepository).findAll();
            verify(topicMapper).toDtoList(topics);
        }

        @Test
        @DisplayName("Should get active topics successfully")
        void shouldGetActiveTopicsSuccessfully() {
            // Given
            List<Topic> topics = Arrays.asList(testRootTopic);
            List<TopicResponseDto> responseDtos = Arrays.asList(testResponseDto);

            when(topicRepository.findByIsActive(true)).thenReturn(topics);
            when(topicMapper.toDtoList(anyList())).thenReturn(responseDtos);

            // When
            List<TopicResponseDto> result = topicService.getActiveTopics();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);

            verify(topicRepository).findByIsActive(true);
            verify(topicMapper).toDtoList(topics);
        }

        @Test
        @DisplayName("Should get root topics successfully")
        void shouldGetRootTopicsSuccessfully() {
            // Given
            List<Topic> topics = Arrays.asList(testRootTopic);
            List<TopicResponseDto> responseDtos = Arrays.asList(testResponseDto);

            when(topicRepository.findByParentTopicIsNull()).thenReturn(topics);
            when(topicMapper.toDtoList(anyList())).thenReturn(responseDtos);

            // When
            List<TopicResponseDto> result = topicService.getRootTopics();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);

            verify(topicRepository).findByParentTopicIsNull();
            verify(topicMapper).toDtoList(topics);
        }

        @Test
        @DisplayName("Should get child topics successfully")
        void shouldGetChildTopicsSuccessfully() {
            // Given
            List<Topic> childTopics = Arrays.asList(testChildTopic);
            List<TopicResponseDto> responseDtos = Arrays.asList(testResponseDto);

            when(topicRepository.existsById(1L)).thenReturn(true);
            when(topicRepository.findByParentTopicId(1L)).thenReturn(childTopics);
            when(topicMapper.toDtoList(anyList())).thenReturn(responseDtos);

            // When
            List<TopicResponseDto> result = topicService.getChildTopics(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);

            verify(topicRepository).existsById(1L);
            verify(topicRepository).findByParentTopicId(1L);
            verify(topicMapper).toDtoList(childTopics);
        }

        @Test
        @DisplayName("Should throw exception when getting children of non-existent parent")
        void shouldThrowExceptionWhenGettingChildrenOfNonExistentParent() {
            // Given
            when(topicRepository.existsById(999L)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> topicService.getChildTopics(999L))
                    .isInstanceOf(TopicNotFoundException.class)
                    .hasMessageContaining("Topic not found with id: 999");

            verify(topicRepository).existsById(999L);
            verify(topicRepository, never()).findByParentTopicId(any());
        }
    }

    @Nested
    @DisplayName("Update Topic Tests")
    class UpdateTopicTests {

        @Test
        @DisplayName("Should update topic successfully")
        void shouldUpdateTopicSuccessfully() {
            // Given
            when(topicRepository.findByIdWithParent(1L)).thenReturn(Optional.of(testRootTopic));
            when(topicRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(testRootTopic));
            when(topicRepository.findBySlug(anyString())).thenReturn(Optional.of(testRootTopic));
            when(topicRepository.save(any(Topic.class))).thenReturn(testRootTopic);
            when(topicMapper.toDto(any(Topic.class))).thenReturn(testResponseDto);

            // When
            TopicResponseDto result = topicService.updateTopic(1L, testRequestDto);

            // Then
            assertThat(result).isNotNull();
            verify(topicRepository).findByIdWithParent(1L);
            verify(topicMapper).updateEntityFromDto(testRequestDto, testRootTopic);
            verify(topicRepository).save(testRootTopic);
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent topic")
        void shouldThrowExceptionWhenUpdatingNonExistentTopic() {
            // Given
            when(topicRepository.findByIdWithParent(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> topicService.updateTopic(999L, testRequestDto))
                    .isInstanceOf(TopicNotFoundException.class)
                    .hasMessageContaining("Topic not found with id: 999");

            verify(topicRepository).findByIdWithParent(999L);
            verify(topicRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when setting topic as its own parent")
        void shouldThrowExceptionWhenSettingTopicAsItsOwnParent() {
            // Given
            testRequestDto.setParentTopicId(1L);
            when(topicRepository.findByIdWithParent(1L)).thenReturn(Optional.of(testRootTopic));
            when(topicRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(testRootTopic));
            when(topicRepository.findBySlug(anyString())).thenReturn(Optional.of(testRootTopic));

            // When/Then
            assertThatThrownBy(() -> topicService.updateTopic(1L, testRequestDto))
                    .isInstanceOf(InvalidTopicHierarchyException.class)
                    .hasMessageContaining("Circular reference detected");

            verify(topicRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when creating circular reference")
        void shouldThrowExceptionWhenCreatingCircularReference() {
            // Given - Try to make parent (testRootTopic) a child of its own child (testChildTopic)
            testRequestDto.setParentTopicId(2L); // testChildTopic's ID

            when(topicRepository.findByIdWithParent(1L)).thenReturn(Optional.of(testRootTopic));
            when(topicRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(testRootTopic));
            when(topicRepository.findBySlug(anyString())).thenReturn(Optional.of(testRootTopic));
            when(topicRepository.findById(2L)).thenReturn(Optional.of(testChildTopic));

            // When/Then
            assertThatThrownBy(() -> topicService.updateTopic(1L, testRequestDto))
                    .isInstanceOf(InvalidTopicHierarchyException.class)
                    .hasMessageContaining("Circular reference detected");

            verify(topicRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when updating with duplicate name")
        void shouldThrowExceptionWhenUpdatingWithDuplicateName() {
            // Given
            Topic anotherTopic = new Topic();
            anotherTopic.setId(2L);
            anotherTopic.setName("Technology");

            when(topicRepository.findByIdWithParent(1L)).thenReturn(Optional.of(testRootTopic));
            when(topicRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(anotherTopic));

            // When/Then
            assertThatThrownBy(() -> topicService.updateTopic(1L, testRequestDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Topic with name 'Technology' already exists");

            verify(topicRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when new parent exceeds max depth")
        void shouldThrowExceptionWhenNewParentExceedsMaxDepth() {
            // Given - Create a deep hierarchy at max depth
            Topic depth1 = new Topic();
            depth1.setId(10L);
            depth1.setParentTopic(null);

            Topic depth2 = new Topic();
            depth2.setId(11L);
            depth2.setParentTopic(depth1);

            Topic depth3 = new Topic();
            depth3.setId(12L);
            depth3.setParentTopic(depth2);

            Topic depth4 = new Topic();
            depth4.setId(13L);
            depth4.setParentTopic(depth3);

            testRequestDto.setParentTopicId(13L);

            when(topicRepository.findByIdWithParent(1L)).thenReturn(Optional.of(testRootTopic));
            when(topicRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(testRootTopic));
            when(topicRepository.findBySlug(anyString())).thenReturn(Optional.of(testRootTopic));
            when(topicRepository.findById(13L)).thenReturn(Optional.of(depth4));

            // When/Then
            assertThatThrownBy(() -> topicService.updateTopic(1L, testRequestDto))
                    .isInstanceOf(InvalidTopicHierarchyException.class)
                    .hasMessageContaining("exceeds maximum depth of 4");

            verify(topicRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Topic Tests")
    class DeleteTopicTests {

        @Test
        @DisplayName("Should delete topic successfully when it has no children")
        void shouldDeleteTopicSuccessfully() {
            // Given
            when(topicRepository.findByIdWithChildren(1L)).thenReturn(Optional.of(testRootTopic));
            doNothing().when(topicRepository).deleteById(1L);

            // When
            topicService.deleteTopic(1L);

            // Then
            verify(topicRepository).findByIdWithChildren(1L);
            verify(topicRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent topic")
        void shouldThrowExceptionWhenDeletingNonExistentTopic() {
            // Given
            when(topicRepository.findByIdWithChildren(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> topicService.deleteTopic(999L))
                    .isInstanceOf(TopicNotFoundException.class)
                    .hasMessageContaining("Topic not found with id: 999");

            verify(topicRepository).findByIdWithChildren(999L);
            verify(topicRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw exception when deleting topic with children")
        void shouldThrowExceptionWhenDeletingTopicWithChildren() {
            // Given
            testRootTopic.getChildTopics().add(testChildTopic);
            when(topicRepository.findByIdWithChildren(1L)).thenReturn(Optional.of(testRootTopic));

            // When/Then
            assertThatThrownBy(() -> topicService.deleteTopic(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot delete topic with id 1 because it has 1 child topics");

            verify(topicRepository).findByIdWithChildren(1L);
            verify(topicRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("Should get topic entity by ID successfully")
        void shouldGetTopicEntityByIdSuccessfully() {
            // Given
            when(topicRepository.findById(1L)).thenReturn(Optional.of(testRootTopic));

            // When
            Topic result = topicService.getTopicEntityById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Technology");

            verify(topicRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw exception when getting entity of non-existent topic")
        void shouldThrowExceptionWhenGettingEntityOfNonExistentTopic() {
            // Given
            when(topicRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> topicService.getTopicEntityById(999L))
                    .isInstanceOf(TopicNotFoundException.class)
                    .hasMessageContaining("Topic not found with id: 999");

            verify(topicRepository).findById(999L);
        }
    }
}