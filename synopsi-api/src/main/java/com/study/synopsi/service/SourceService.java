package com.study.synopsi.service;

import com.study.synopsi.dto.SourceRequestDto;
import com.study.synopsi.dto.SourceResponseDto;
import com.study.synopsi.exception.SourceNotFoundException;
import com.study.synopsi.mapper.SourceMapper;
import com.study.synopsi.model.Source;
import com.study.synopsi.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SourceService {

    private final SourceRepository sourceRepository;
    private final SourceMapper sourceMapper;

    /**
     * Create a new source
     */
    @Transactional
    public SourceResponseDto createSource(SourceRequestDto dto) {
        log.debug("Creating new source: {}", dto.getName());

        // Validate uniqueness
        if (sourceRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Source with name '" + dto.getName() + "' already exists");
        }
        if (sourceRepository.existsByBaseUrl(dto.getBaseUrl())) {
            throw new IllegalArgumentException("Source with base URL '" + dto.getBaseUrl() + "' already exists");
        }

        // Validate credibility score
        validateCredibilityScore(dto.getCredibilityScore());

        Source source = sourceMapper.toEntity(dto);
        Source savedSource = sourceRepository.save(source);

        log.info("Created source with id: {}", savedSource.getId());
        return sourceMapper.toDto(savedSource);
    }

    /**
     * Get source by ID
     */
    @Transactional(readOnly = true)
    public SourceResponseDto getSourceById(Long id) {
        log.debug("Fetching source with id: {}", id);
        Source source = sourceRepository.findById(id)
                .orElseThrow(() -> new SourceNotFoundException(id));
        return sourceMapper.toDto(source);
    }

    /**
     * Get source by name
     */
    @Transactional(readOnly = true)
    public SourceResponseDto getSourceByName(String name) {
        log.debug("Fetching source with name: {}", name);
        Source source = sourceRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new SourceNotFoundException(name));
        return sourceMapper.toDto(source);
    }

    /**
     * Get source by ID with feeds loaded
     */
    @Transactional(readOnly = true)
    public SourceResponseDto getSourceByIdWithFeeds(Long id) {
        log.debug("Fetching source with id and feeds: {}", id);
        Source source = sourceRepository.findByIdWithFeeds(id)
                .orElseThrow(() -> new SourceNotFoundException(id));
        return sourceMapper.toDto(source);
    }

    /**
     * Get all sources
     */
    @Transactional(readOnly = true)
    public List<SourceResponseDto> getAllSources() {
        log.debug("Fetching all sources");
        List<Source> sources = sourceRepository.findAll();
        return sourceMapper.toDtoList(sources);
    }

    /**
     * Get all active sources
     */
    @Transactional(readOnly = true)
    public List<SourceResponseDto> getActiveSources() {
        log.debug("Fetching active sources");
        List<Source> sources = sourceRepository.findByIsActive(true);
        return sourceMapper.toDtoList(sources);
    }

    /**
     * Get all sources with feeds loaded
     */
    @Transactional(readOnly = true)
    public List<SourceResponseDto> getAllSourcesWithFeeds() {
        log.debug("Fetching all sources with feeds");
        List<Source> sources = sourceRepository.findAllWithFeeds();
        return sourceMapper.toDtoList(sources);
    }

    /**
     * Get sources by type
     */
    @Transactional(readOnly = true)
    public List<SourceResponseDto> getSourcesByType(Source.SourceType sourceType) {
        log.debug("Fetching sources by type: {}", sourceType);
        List<Source> sources = sourceRepository.findBySourceType(sourceType);
        return sourceMapper.toDtoList(sources);
    }

    /**
     * Update an existing source
     */
    @Transactional
    public SourceResponseDto updateSource(Long id, SourceRequestDto dto) {
        log.debug("Updating source with id: {}", id);

        Source existingSource = sourceRepository.findById(id)
                .orElseThrow(() -> new SourceNotFoundException(id));

        // Validate uniqueness (excluding current source)
        sourceRepository.findByNameIgnoreCase(dto.getName())
                .ifPresent(source -> {
                    if (!source.getId().equals(id)) {
                        throw new IllegalArgumentException("Source with name '" + dto.getName() + "' already exists");
                    }
                });

        sourceRepository.findByBaseUrl(dto.getBaseUrl())
                .ifPresent(source -> {
                    if (!source.getId().equals(id)) {
                        throw new IllegalArgumentException("Source with base URL '" + dto.getBaseUrl() + "' already exists");
                    }
                });

        // Validate credibility score
        validateCredibilityScore(dto.getCredibilityScore());

        sourceMapper.updateEntityFromDto(dto, existingSource);
        Source updatedSource = sourceRepository.save(existingSource);

        log.info("Updated source with id: {}", id);
        return sourceMapper.toDto(updatedSource);
    }

    /**
     * Activate a source
     */
    @Transactional
    public SourceResponseDto activateSource(Long id) {
        log.debug("Activating source with id: {}", id);
        Source source = sourceRepository.findById(id)
                .orElseThrow(() -> new SourceNotFoundException(id));
        
        source.setIsActive(true);
        Source updatedSource = sourceRepository.save(source);
        
        log.info("Activated source with id: {}", id);
        return sourceMapper.toDto(updatedSource);
    }

    /**
     * Deactivate a source
     */
    @Transactional
    public SourceResponseDto deactivateSource(Long id) {
        log.debug("Deactivating source with id: {}", id);
        Source source = sourceRepository.findById(id)
                .orElseThrow(() -> new SourceNotFoundException(id));
        
        source.setIsActive(false);
        Source updatedSource = sourceRepository.save(source);
        
        log.info("Deactivated source with id: {}", id);
        return sourceMapper.toDto(updatedSource);
    }

    /**
     * Delete a source
     */
    @Transactional
    public void deleteSource(Long id) {
        log.debug("Deleting source with id: {}", id);
        
        if (!sourceRepository.existsById(id)) {
            throw new SourceNotFoundException(id);
        }
        
        sourceRepository.deleteById(id);
        log.info("Deleted source with id: {}", id);
    }

    /**
     * Validate credibility score is between 0.0 and 1.0
     */
    private void validateCredibilityScore(Double credibilityScore) {
        if (credibilityScore != null && (credibilityScore < 0.0 || credibilityScore > 1.0)) {
            throw new IllegalArgumentException("Credibility score must be between 0.0 and 1.0");
        }
    }
}