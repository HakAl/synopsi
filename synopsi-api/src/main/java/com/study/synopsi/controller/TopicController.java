package com.study.synopsi.controller;

import com.study.synopsi.dto.TopicRequestDto;
import com.study.synopsi.dto.TopicResponseDto;
import com.study.synopsi.service.TopicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
@Slf4j
public class TopicController {

    private final TopicService topicService;

    /**
     * Create a new topic
     * POST /api/v1/topics
     */
    @PostMapping
    public ResponseEntity<TopicResponseDto> createTopic(@Valid @RequestBody TopicRequestDto requestDto) {
        log.info("POST /api/v1/topics - Creating new topic: {}", requestDto.getName());
        TopicResponseDto response = topicService.createTopic(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get topic by ID
     * GET /api/v1/topics/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TopicResponseDto> getTopicById(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") Boolean includeChildren) {
        log.info("GET /api/v1/topics/{} - Fetching topic (includeChildren={})", id, includeChildren);
        
        TopicResponseDto response;
        if (includeChildren) {
            response = topicService.getTopicByIdWithChildren(id);
        } else {
            response = topicService.getTopicById(id);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get topic by slug
     * GET /api/v1/topics/by-slug/{slug}
     */
    @GetMapping("/by-slug/{slug}")
    public ResponseEntity<TopicResponseDto> getTopicBySlug(@PathVariable String slug) {
        log.info("GET /api/v1/topics/by-slug/{} - Fetching topic by slug", slug);
        TopicResponseDto response = topicService.getTopicBySlug(slug);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all topics
     * GET /api/v1/topics
     */
    @GetMapping
    public ResponseEntity<List<TopicResponseDto>> getAllTopics(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false, defaultValue = "false") Boolean rootOnly) {
        
        log.info("GET /api/v1/topics - Fetching topics (active={}, rootOnly={})", active, rootOnly);

        List<TopicResponseDto> response;

        // Filter by root topics only
        if (rootOnly) {
            response = topicService.getRootTopics();
        }
        // Filter by active status
        else if (active != null && active) {
            response = topicService.getActiveTopics();
        }
        // Get all
        else {
            response = topicService.getAllTopics();
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get root topics (topics without parent)
     * GET /api/v1/topics/root
     */
    @GetMapping("/root")
    public ResponseEntity<List<TopicResponseDto>> getRootTopics(
            @RequestParam(required = false, defaultValue = "false") Boolean includeChildren) {
        log.info("GET /api/v1/topics/root - Fetching root topics (includeChildren={})", includeChildren);
        
        List<TopicResponseDto> response;
        if (includeChildren) {
            response = topicService.getRootTopicsWithChildren();
        } else {
            response = topicService.getRootTopics();
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get child topics of a parent topic
     * GET /api/v1/topics/{parentId}/children
     */
    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<TopicResponseDto>> getChildTopics(@PathVariable Long parentId) {
        log.info("GET /api/v1/topics/{}/children - Fetching child topics", parentId);
        List<TopicResponseDto> response = topicService.getChildTopics(parentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing topic
     * PUT /api/v1/topics/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<TopicResponseDto> updateTopic(
            @PathVariable Long id,
            @Valid @RequestBody TopicRequestDto requestDto) {
        log.info("PUT /api/v1/topics/{} - Updating topic", id);
        TopicResponseDto response = topicService.updateTopic(id, requestDto);
        return ResponseEntity.ok(response);
    }

    /**
     * Partially update a topic
     * PATCH /api/v1/topics/{id}
     */
    @PatchMapping("/{id}")
    public ResponseEntity<TopicResponseDto> partialUpdateTopic(
            @PathVariable Long id,
            @RequestBody TopicRequestDto requestDto) {
        log.info("PATCH /api/v1/topics/{} - Partially updating topic", id);
        // Note: Use same update method - MapStruct will handle null values with IGNORE strategy
        TopicResponseDto response = topicService.updateTopic(id, requestDto);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a topic
     * DELETE /api/v1/topics/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTopic(@PathVariable Long id) {
        log.info("DELETE /api/v1/topics/{} - Deleting topic", id);
        topicService.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }
}