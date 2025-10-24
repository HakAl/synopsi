package com.study.synopsi.repository;

import com.study.synopsi.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    /**
     * Find topic by slug
     */
    Optional<Topic> findBySlug(String slug);

    /**
     * Find topic by name (case-insensitive)
     */
    Optional<Topic> findByNameIgnoreCase(String name);

    /**
     * Find all active topics
     */
    List<Topic> findByIsActive(Boolean isActive);

    /**
     * Find all root topics (topics without parent)
     */
    List<Topic> findByParentTopicIsNull();

    /**
     * Find all child topics of a parent
     */
    List<Topic> findByParentTopicId(Long parentTopicId);

    /**
     * Find topic by ID with parent eagerly loaded
     */
    @Query("SELECT t FROM Topic t LEFT JOIN FETCH t.parentTopic WHERE t.id = :id")
    Optional<Topic> findByIdWithParent(@Param("id") Long id);

    /**
     * Find topic by slug with parent eagerly loaded
     */
    @Query("SELECT t FROM Topic t LEFT JOIN FETCH t.parentTopic WHERE t.slug = :slug")
    Optional<Topic> findBySlugWithParent(@Param("slug") String slug);

    /**
     * Find topic by ID with child topics eagerly loaded
     */
    @Query("SELECT t FROM Topic t LEFT JOIN FETCH t.childTopics WHERE t.id = :id")
    Optional<Topic> findByIdWithChildren(@Param("id") Long id);

    /**
     * Find topic by ID with both parent and children eagerly loaded
     */
    @Query("SELECT t FROM Topic t " +
           "LEFT JOIN FETCH t.parentTopic " +
           "LEFT JOIN FETCH t.childTopics " +
           "WHERE t.id = :id")
    Optional<Topic> findByIdWithParentAndChildren(@Param("id") Long id);

    /**
     * Find all root topics with children eagerly loaded
     */
    @Query("SELECT DISTINCT t FROM Topic t " +
           "LEFT JOIN FETCH t.childTopics " +
           "WHERE t.parentTopic IS NULL")
    List<Topic> findRootTopicsWithChildren();

    /**
     * Check if topic exists by slug (for validation)
     */
    boolean existsBySlug(String slug);

    /**
     * Check if topic exists by name (for validation)
     */
    boolean existsByNameIgnoreCase(String name);
}