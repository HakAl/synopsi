package com.study.synopsi.controller;

import com.study.synopsi.dto.SourceRequestDto;
import com.study.synopsi.dto.SourceResponseDto;
import com.study.synopsi.model.Source;
import com.study.synopsi.service.SourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sources")
@RequiredArgsConstructor
@Slf4j
public class SourceController {

    private final SourceService sourceService;

    /**
     * Create a new source
     * POST /api/sources
     */
    @PostMapping
    public ResponseEntity<SourceResponseDto> createSource(@Valid @RequestBody SourceRequestDto requestDto) {
        log.info("POST /api/sources - Creating new source: {}", requestDto.getName());
        SourceResponseDto response = sourceService.createSource(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get source by ID
     * GET /api/sources/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<SourceResponseDto> getSourceById(@PathVariable Long id) {
        log.info("GET /api/sources/{} - Fetching source", id);
        SourceResponseDto response = sourceService.getSourceById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get source by ID with feeds loaded
     * GET /api/sources/{id}?includeFeeds=true
     */
    @GetMapping("/{id}/with-feeds")
    public ResponseEntity<SourceResponseDto> getSourceByIdWithFeeds(@PathVariable Long id) {
        log.info("GET /api/sources/{}/with-feeds - Fetching source with feeds", id);
        SourceResponseDto response = sourceService.getSourceByIdWithFeeds(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get source by name
     * GET /api/sources/by-name/{name}
     */
    @GetMapping("/by-name/{name}")
    public ResponseEntity<SourceResponseDto> getSourceByName(@PathVariable String name) {
        log.info("GET /api/sources/by-name/{} - Fetching source by name", name);
        SourceResponseDto response = sourceService.getSourceByName(name);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all sources
     * GET /api/sources
     */
    @GetMapping
    public ResponseEntity<List<SourceResponseDto>> getAllSources(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Source.SourceType type,
            @RequestParam(required = false, defaultValue = "false") Boolean includeFeeds) {
        
        log.info("GET /api/sources - Fetching sources (active={}, type={}, includeFeeds={})", 
                active, type, includeFeeds);

        List<SourceResponseDto> response;

        // Filter by active status
        if (active != null && active) {
            response = sourceService.getActiveSources();
        }
        // Filter by type
        else if (type != null) {
            response = sourceService.getSourcesByType(type);
        }
        // Include feeds
        else if (includeFeeds) {
            response = sourceService.getAllSourcesWithFeeds();
        }
        // Get all
        else {
            response = sourceService.getAllSources();
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing source
     * PUT /api/sources/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<SourceResponseDto> updateSource(
            @PathVariable Long id,
            @Valid @RequestBody SourceRequestDto requestDto) {
        log.info("PUT /api/sources/{} - Updating source", id);
        SourceResponseDto response = sourceService.updateSource(id, requestDto);
        return ResponseEntity.ok(response);
    }

    /**
     * Partially update a source
     * PATCH /api/sources/{id}
     */
    @PatchMapping("/{id}")
    public ResponseEntity<SourceResponseDto> partialUpdateSource(
            @PathVariable Long id,
            @RequestBody SourceRequestDto requestDto) {
        log.info("PATCH /api/sources/{} - Partially updating source", id);
        // Note: Use same update method - MapStruct will handle null values with IGNORE strategy
        SourceResponseDto response = sourceService.updateSource(id, requestDto);
        return ResponseEntity.ok(response);
    }

    /**
     * Activate a source
     * PATCH /api/sources/{id}/activate
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<SourceResponseDto> activateSource(@PathVariable Long id) {
        log.info("PATCH /api/sources/{}/activate - Activating source", id);
        SourceResponseDto response = sourceService.activateSource(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate a source
     * PATCH /api/sources/{id}/deactivate
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<SourceResponseDto> deactivateSource(@PathVariable Long id) {
        log.info("PATCH /api/sources/{}/deactivate - Deactivating source", id);
        SourceResponseDto response = sourceService.deactivateSource(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a source
     * DELETE /api/sources/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSource(@PathVariable Long id) {
        log.info("DELETE /api/sources/{} - Deleting source", id);
        sourceService.deleteSource(id);
        return ResponseEntity.noContent().build();
    }
}