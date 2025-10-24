package com.study.synopsi.service;

import com.study.synopsi.dto.TopicRequestDto;
import com.study.synopsi.dto.TopicResponseDto;
import com.study.synopsi.exception.InvalidTopicHierarchyException;
import com.study.synopsi.exception.TopicNotFoundException;
import com.study.synopsi.mapper.TopicMapper;
import com.study.synopsi.model.Topic;
import com.study.synopsi.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TopicService {

    private final TopicRepository topicRepository;
    private final TopicMapper topicMapper;

    private static final int MAX_HIERARCHY_DEPTH = 4;

    /**
     * Create a new topic
     */
    @Transactional
    public TopicResponseDto createTopic(TopicRequestDto dto) {
        log.debug("Creating new topic: {}", dto.getName());

        // Validate uniqueness
        if (topicRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Topic with name '" + dto.getName() + "' already exists");
        }
        if (topicRepository.existsBySlug(dto.getSlug())) {
            throw new IllegalArgumentException("Topic with slug '" + dto.getSlug() + "' already exists");
        }

        // Validate parent topic if provided
        if (dto.getParentTopicId() != null) {
            Topic parentTopic = topicRepository.findById(dto.getParentTopicId())
                    .orElseThrow(() -> new TopicNotFoundException(dto.getParentTopicId()));
            
            // Validate hierarchy depth
            int parentDepth = calculateDepth(parentTopic);
            if (parentDepth >= MAX_HIERARCHY_DEPTH - 1) {
                throw InvalidTopicHierarchyException.maxDepthExceeded(MAX_HIERARCHY_DEPTH);
            }
        }

        Topic topic = topicMapper.toEntity(dto);
        Topic savedTopic = topicRepository.save(topic);

        log.info("Created topic with id: {}", savedTopic.getId());
        return topicMapper.toDto(savedTopic);
    }

    /**
     * Get topic by ID
     */
    @Transactional(readOnly = true)
    public TopicResponseDto getTopicById(Long id) {
        log.debug("Fetching topic with id: {}", id);
        Topic topic = topicRepository.findByIdWithParent(id)
                .orElseThrow(() -> new TopicNotFoundException(id));
        return topicMapper.toDto(topic);
    }

    /**
     * Get topic by slug
     */
    @Transactional(readOnly = true)
    public TopicResponseDto getTopicBySlug(String slug) {
        log.debug("Fetching topic with slug: {}", slug);
        Topic topic = topicRepository.findBySlugWithParent(slug)
                .orElseThrow(() -> new TopicNotFoundException(slug));
        return topicMapper.toDto(topic);
    }

    /**
     * Get topic by ID with children loaded
     */
    @Transactional(readOnly = true)
    public TopicResponseDto getTopicByIdWithChildren(Long id) {
        log.debug("Fetching topic with id and children: {}", id);
        Topic topic = topicRepository.findByIdWithChildren(id)
                .orElseThrow(() -> new TopicNotFoundException(id));
        return topicMapper.toDto(topic);
    }

    /**
     * Get all topics
     */
    @Transactional(readOnly = true)
    public List<TopicResponseDto> getAllTopics() {
        log.debug("Fetching all topics");
        List<Topic> topics = topicRepository.findAll();
        return topicMapper.toDtoList(topics);
    }

    /**
     * Get all active topics
     */
    @Transactional(readOnly = true)
    public List<TopicResponseDto> getActiveTopics() {
        log.debug("Fetching active topics");
        List<Topic> topics = topicRepository.findByIsActive(true);
        return topicMapper.toDtoList(topics);
    }

    /**
     * Get all root topics (topics without parent)
     */
    @Transactional(readOnly = true)
    public List<TopicResponseDto> getRootTopics() {
        log.debug("Fetching root topics");
        List<Topic> topics = topicRepository.findByParentTopicIsNull();
        return topicMapper.toDtoList(topics);
    }

    /**
     * Get all root topics with children
     */
    @Transactional(readOnly = true)
    public List<TopicResponseDto> getRootTopicsWithChildren() {
        log.debug("Fetching root topics with children");
        List<Topic> topics = topicRepository.findRootTopicsWithChildren();
        return topicMapper.toDtoList(topics);
    }

    /**
     * Get child topics of a parent topic
     */
    @Transactional(readOnly = true)
    public List<TopicResponseDto> getChildTopics(Long parentTopicId) {
        log.debug("Fetching child topics of parent: {}", parentTopicId);
        
        // Verify parent exists
        if (!topicRepository.existsById(parentTopicId)) {
            throw new TopicNotFoundException(parentTopicId);
        }
        
        List<Topic> topics = topicRepository.findByParentTopicId(parentTopicId);
        return topicMapper.toDtoList(topics);
    }

    /**
     * Update an existing topic
     */
    @Transactional
    public TopicResponseDto updateTopic(Long id, TopicRequestDto dto) {
        log.debug("Updating topic with id: {}", id);

        Topic existingTopic = topicRepository.findByIdWithParent(id)
                .orElseThrow(() -> new TopicNotFoundException(id));

        // Validate uniqueness (excluding current topic)
        topicRepository.findByNameIgnoreCase(dto.getName())
                .ifPresent(topic -> {
                    if (!topic.getId().equals(id)) {
                        throw new IllegalArgumentException("Topic with name '" + dto.getName() + "' already exists");
                    }
                });

        topicRepository.findBySlug(dto.getSlug())
                .ifPresent(topic -> {
                    if (!topic.getId().equals(id)) {
                        throw new IllegalArgumentException("Topic with slug '" + dto.getSlug() + "' already exists");
                    }
                });

        // Validate parent topic changes
        if (dto.getParentTopicId() != null) {
            // Check if trying to set itself as parent
            if (dto.getParentTopicId().equals(id)) {
                throw InvalidTopicHierarchyException.circularReference(id, dto.getParentTopicId());
            }

            Topic newParent = topicRepository.findById(dto.getParentTopicId())
                    .orElseThrow(() -> new TopicNotFoundException(dto.getParentTopicId()));

            // Check for circular references (new parent cannot be a descendant)
            if (isDescendant(existingTopic, newParent)) {
                throw InvalidTopicHierarchyException.circularReference(id, dto.getParentTopicId());
            }

            // Validate hierarchy depth with new parent
            int newParentDepth = calculateDepth(newParent);
            if (newParentDepth >= MAX_HIERARCHY_DEPTH - 1) {
                throw InvalidTopicHierarchyException.maxDepthExceeded(MAX_HIERARCHY_DEPTH);
            }
        }

        topicMapper.updateEntityFromDto(dto, existingTopic);
        Topic updatedTopic = topicRepository.save(existingTopic);

        log.info("Updated topic with id: {}", id);
        return topicMapper.toDto(updatedTopic);
    }

    /**
     * Delete a topic
     */
    @Transactional
    public void deleteTopic(Long id) {
        log.debug("Deleting topic with id: {}", id);
        
        Topic topic = topicRepository.findByIdWithChildren(id)
                .orElseThrow(() -> new TopicNotFoundException(id));

        // Check if topic has children
        if (!topic.getChildTopics().isEmpty()) {
            throw new IllegalArgumentException(
                "Cannot delete topic with id " + id + " because it has " + 
                topic.getChildTopics().size() + " child topics"
            );
        }
        
        topicRepository.deleteById(id);
        log.info("Deleted topic with id: {}", id);
    }

    /**
     * Calculate the depth of a topic in the hierarchy
     */
    private int calculateDepth(Topic topic) {
        int depth = 0;
        Topic current = topic;
        while (current.getParentTopic() != null) {
            depth++;
            current = current.getParentTopic();
            
            // Safety check to prevent infinite loops
            if (depth > MAX_HIERARCHY_DEPTH * 2) {
                log.error("Circular reference detected in topic hierarchy for topic: {}", topic.getId());
                throw new InvalidTopicHierarchyException("Circular reference detected in topic hierarchy");
            }
        }
        return depth;
    }

    /**
     * Check if potentialDescendant is a descendant of ancestor
     */
    private boolean isDescendant(Topic ancestor, Topic potentialDescendant) {
        Set<Long> visited = new HashSet<>();
        Topic current = potentialDescendant;
        
        while (current != null) {
            if (current.getId().equals(ancestor.getId())) {
                return true;
            }
            
            // Prevent infinite loops
            if (visited.contains(current.getId())) {
                log.error("Circular reference detected while checking descendant relationship");
                return true; // Treat as circular reference
            }
            visited.add(current.getId());
            
            current = current.getParentTopic();
        }
        
        return false;
    }

    /**
     * Validate that a topic exists and return it (used by FeedService)
     */
    @Transactional(readOnly = true)
    public Topic getTopicEntityById(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new TopicNotFoundException(id));
    }
}