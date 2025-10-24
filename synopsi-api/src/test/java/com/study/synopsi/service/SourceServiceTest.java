package com.study.synopsi.service;

import com.study.synopsi.dto.SourceRequestDto;
import com.study.synopsi.dto.SourceResponseDto;
import com.study.synopsi.exception.SourceNotFoundException;
import com.study.synopsi.mapper.SourceMapper;
import com.study.synopsi.model.Source;
import com.study.synopsi.repository.SourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SourceService Tests")
class SourceServiceTest {

    @Mock
    private SourceRepository sourceRepository;

    @Mock
    private SourceMapper sourceMapper;

    @InjectMocks
    private SourceService sourceService;

    private Source testSource;
    private SourceRequestDto testRequestDto;
    private SourceResponseDto testResponseDto;

    @BeforeEach
    void setUp() {
        // Setup test data
        testSource = new Source();
        testSource.setId(1L);
        testSource.setName("TechCrunch");
        testSource.setBaseUrl("https://techcrunch.com");
        testSource.setDescription("Leading technology news site");
        testSource.setCredibilityScore(0.85);
        testSource.setLanguage("en");
        testSource.setCountry("US");
        testSource.setSourceType(Source.SourceType.NEWS);
        testSource.setIsActive(true);
        testSource.setCreatedAt(LocalDateTime.now());
        testSource.setUpdatedAt(LocalDateTime.now());

        testRequestDto = SourceRequestDto.builder()
                .name("TechCrunch")
                .baseUrl("https://techcrunch.com")
                .description("Leading technology news site")
                .credibilityScore(0.85)
                .language("en")
                .country("US")
                .sourceType(Source.SourceType.NEWS)
                .isActive(true)
                .build();

        testResponseDto = SourceResponseDto.builder()
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
                .createdAt(testSource.getCreatedAt())
                .updatedAt(testSource.getUpdatedAt())
                .build();
    }

    @Nested
    @DisplayName("Create Source Tests")
    class CreateSourceTests {

        @Test
        @DisplayName("Should create source successfully")
        void shouldCreateSourceSuccessfully() {
            // Given
            when(sourceRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(sourceRepository.existsByBaseUrl(anyString())).thenReturn(false);
            when(sourceMapper.toEntity(any(SourceRequestDto.class))).thenReturn(testSource);
            when(sourceRepository.save(any(Source.class))).thenReturn(testSource);
            when(sourceMapper.toDto(any(Source.class))).thenReturn(testResponseDto);

            // When
            SourceResponseDto result = sourceService.createSource(testRequestDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("TechCrunch");
            assertThat(result.getCredibilityScore()).isEqualTo(0.85);

            verify(sourceRepository).existsByNameIgnoreCase("TechCrunch");
            verify(sourceRepository).existsByBaseUrl("https://techcrunch.com");
            verify(sourceRepository).save(any(Source.class));
            verify(sourceMapper).toEntity(testRequestDto);
            verify(sourceMapper).toDto(testSource);
        }

        @Test
        @DisplayName("Should throw exception when name already exists")
        void shouldThrowExceptionWhenNameExists() {
            // Given
            when(sourceRepository.existsByNameIgnoreCase(anyString())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> sourceService.createSource(testRequestDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Source with name 'TechCrunch' already exists");

            verify(sourceRepository).existsByNameIgnoreCase("TechCrunch");
            verify(sourceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when base URL already exists")
        void shouldThrowExceptionWhenBaseUrlExists() {
            // Given
            when(sourceRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(sourceRepository.existsByBaseUrl(anyString())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> sourceService.createSource(testRequestDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Source with base URL 'https://techcrunch.com' already exists");

            verify(sourceRepository).existsByBaseUrl("https://techcrunch.com");
            verify(sourceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when credibility score is invalid (too low)")
        void shouldThrowExceptionWhenCredibilityScoreTooLow() {
            // Given
            testRequestDto.setCredibilityScore(-0.1);
            when(sourceRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(sourceRepository.existsByBaseUrl(anyString())).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> sourceService.createSource(testRequestDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Credibility score must be between 0.0 and 1.0");

            verify(sourceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when credibility score is invalid (too high)")
        void shouldThrowExceptionWhenCredibilityScoreTooHigh() {
            // Given
            testRequestDto.setCredibilityScore(1.5);
            when(sourceRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(sourceRepository.existsByBaseUrl(anyString())).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> sourceService.createSource(testRequestDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Credibility score must be between 0.0 and 1.0");

            verify(sourceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should accept null credibility score")
        void shouldAcceptNullCredibilityScore() {
            // Given
            testRequestDto.setCredibilityScore(null);
            when(sourceRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(sourceRepository.existsByBaseUrl(anyString())).thenReturn(false);
            when(sourceMapper.toEntity(any(SourceRequestDto.class))).thenReturn(testSource);
            when(sourceRepository.save(any(Source.class))).thenReturn(testSource);
            when(sourceMapper.toDto(any(Source.class))).thenReturn(testResponseDto);

            // When
            SourceResponseDto result = sourceService.createSource(testRequestDto);

            // Then
            assertThat(result).isNotNull();
            verify(sourceRepository).save(any(Source.class));
        }
    }

    @Nested
    @DisplayName("Get Source Tests")
    class GetSourceTests {

        @Test
        @DisplayName("Should get source by ID successfully")
        void shouldGetSourceByIdSuccessfully() {
            // Given
            when(sourceRepository.findById(1L)).thenReturn(Optional.of(testSource));
            when(sourceMapper.toDto(any(Source.class))).thenReturn(testResponseDto);

            // When
            SourceResponseDto result = sourceService.getSourceById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("TechCrunch");

            verify(sourceRepository).findById(1L);
            verify(sourceMapper).toDto(testSource);
        }

        @Test
        @DisplayName("Should throw exception when source not found by ID")
        void shouldThrowExceptionWhenSourceNotFoundById() {
            // Given
            when(sourceRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> sourceService.getSourceById(999L))
                    .isInstanceOf(SourceNotFoundException.class)
                    .hasMessageContaining("Source not found with id: 999");

            verify(sourceRepository).findById(999L);
            verify(sourceMapper, never()).toDto(any());
        }

        @Test
        @DisplayName("Should get source by name successfully")
        void shouldGetSourceByNameSuccessfully() {
            // Given
            when(sourceRepository.findByNameIgnoreCase("TechCrunch")).thenReturn(Optional.of(testSource));
            when(sourceMapper.toDto(any(Source.class))).thenReturn(testResponseDto);

            // When
            SourceResponseDto result = sourceService.getSourceByName("TechCrunch");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("TechCrunch");

            verify(sourceRepository).findByNameIgnoreCase("TechCrunch");
            verify(sourceMapper).toDto(testSource);
        }

        @Test
        @DisplayName("Should get source by ID with feeds")
        void shouldGetSourceByIdWithFeeds() {
            // Given
            when(sourceRepository.findByIdWithFeeds(1L)).thenReturn(Optional.of(testSource));
            when(sourceMapper.toDto(any(Source.class))).thenReturn(testResponseDto);

            // When
            SourceResponseDto result = sourceService.getSourceByIdWithFeeds(1L);

            // Then
            assertThat(result).isNotNull();
            verify(sourceRepository).findByIdWithFeeds(1L);
            verify(sourceMapper).toDto(testSource);
        }

        @Test
        @DisplayName("Should get all sources successfully")
        void shouldGetAllSourcesSuccessfully() {
            // Given
            List<Source> sources = Arrays.asList(testSource, testSource);
            List<SourceResponseDto> responseDtos = Arrays.asList(testResponseDto, testResponseDto);

            when(sourceRepository.findAll()).thenReturn(sources);
            when(sourceMapper.toDtoList(anyList())).thenReturn(responseDtos);

            // When
            List<SourceResponseDto> result = sourceService.getAllSources();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);

            verify(sourceRepository).findAll();
            verify(sourceMapper).toDtoList(sources);
        }

        @Test
        @DisplayName("Should get active sources successfully")
        void shouldGetActiveSourcesSuccessfully() {
            // Given
            List<Source> sources = Arrays.asList(testSource);
            List<SourceResponseDto> responseDtos = Arrays.asList(testResponseDto);

            when(sourceRepository.findByIsActive(true)).thenReturn(sources);
            when(sourceMapper.toDtoList(anyList())).thenReturn(responseDtos);

            // When
            List<SourceResponseDto> result = sourceService.getActiveSources();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIsActive()).isTrue();

            verify(sourceRepository).findByIsActive(true);
            verify(sourceMapper).toDtoList(sources);
        }

        @Test
        @DisplayName("Should get sources by type successfully")
        void shouldGetSourcesByTypeSuccessfully() {
            // Given
            List<Source> sources = Arrays.asList(testSource);
            List<SourceResponseDto> responseDtos = Arrays.asList(testResponseDto);

            when(sourceRepository.findBySourceType(Source.SourceType.NEWS)).thenReturn(sources);
            when(sourceMapper.toDtoList(anyList())).thenReturn(responseDtos);

            // When
            List<SourceResponseDto> result = sourceService.getSourcesByType(Source.SourceType.NEWS);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);

            verify(sourceRepository).findBySourceType(Source.SourceType.NEWS);
            verify(sourceMapper).toDtoList(sources);
        }
    }

    @Nested
    @DisplayName("Update Source Tests")
    class UpdateSourceTests {

        @Test
        @DisplayName("Should update source successfully")
        void shouldUpdateSourceSuccessfully() {
            // Given
            when(sourceRepository.findById(1L)).thenReturn(Optional.of(testSource));
            when(sourceRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(testSource));
            when(sourceRepository.findByBaseUrl(anyString())).thenReturn(Optional.of(testSource));
            when(sourceRepository.save(any(Source.class))).thenReturn(testSource);
            when(sourceMapper.toDto(any(Source.class))).thenReturn(testResponseDto);

            // When
            SourceResponseDto result = sourceService.updateSource(1L, testRequestDto);

            // Then
            assertThat(result).isNotNull();
            verify(sourceRepository).findById(1L);
            verify(sourceMapper).updateEntityFromDto(testRequestDto, testSource);
            verify(sourceRepository).save(testSource);
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent source")
        void shouldThrowExceptionWhenUpdatingNonExistentSource() {
            // Given
            when(sourceRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> sourceService.updateSource(999L, testRequestDto))
                    .isInstanceOf(SourceNotFoundException.class)
                    .hasMessageContaining("Source not found with id: 999");

            verify(sourceRepository).findById(999L);
            verify(sourceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when updating with duplicate name")
        void shouldThrowExceptionWhenUpdatingWithDuplicateName() {
            // Given
            Source anotherSource = new Source();
            anotherSource.setId(2L);
            anotherSource.setName("TechCrunch");

            when(sourceRepository.findById(1L)).thenReturn(Optional.of(testSource));
            when(sourceRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(anotherSource));

            // When/Then
            assertThatThrownBy(() -> sourceService.updateSource(1L, testRequestDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Source with name 'TechCrunch' already exists");

            verify(sourceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Activate/Deactivate Source Tests")
    class ActivateDeactivateSourceTests {

        @Test
        @DisplayName("Should activate source successfully")
        void shouldActivateSourceSuccessfully() {
            // Given
            testSource.setIsActive(false);
            when(sourceRepository.findById(1L)).thenReturn(Optional.of(testSource));
            when(sourceRepository.save(any(Source.class))).thenReturn(testSource);
            when(sourceMapper.toDto(any(Source.class))).thenReturn(testResponseDto);

            // When
            SourceResponseDto result = sourceService.activateSource(1L);

            // Then
            assertThat(result).isNotNull();
            verify(sourceRepository).findById(1L);
            verify(sourceRepository).save(testSource);
            assertThat(testSource.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Should deactivate source successfully")
        void shouldDeactivateSourceSuccessfully() {
            // Given
            when(sourceRepository.findById(1L)).thenReturn(Optional.of(testSource));
            when(sourceRepository.save(any(Source.class))).thenReturn(testSource);
            when(sourceMapper.toDto(any(Source.class))).thenReturn(testResponseDto);

            // When
            SourceResponseDto result = sourceService.deactivateSource(1L);

            // Then
            assertThat(result).isNotNull();
            verify(sourceRepository).findById(1L);
            verify(sourceRepository).save(testSource);
            assertThat(testSource.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("Should throw exception when activating non-existent source")
        void shouldThrowExceptionWhenActivatingNonExistentSource() {
            // Given
            when(sourceRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> sourceService.activateSource(999L))
                    .isInstanceOf(SourceNotFoundException.class);

            verify(sourceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Source Tests")
    class DeleteSourceTests {

        @Test
        @DisplayName("Should delete source successfully")
        void shouldDeleteSourceSuccessfully() {
            // Given
            when(sourceRepository.existsById(1L)).thenReturn(true);
            doNothing().when(sourceRepository).deleteById(1L);

            // When
            sourceService.deleteSource(1L);

            // Then
            verify(sourceRepository).existsById(1L);
            verify(sourceRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent source")
        void shouldThrowExceptionWhenDeletingNonExistentSource() {
            // Given
            when(sourceRepository.existsById(999L)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> sourceService.deleteSource(999L))
                    .isInstanceOf(SourceNotFoundException.class)
                    .hasMessageContaining("Source not found with id: 999");

            verify(sourceRepository).existsById(999L);
            verify(sourceRepository, never()).deleteById(any());
        }
    }
}