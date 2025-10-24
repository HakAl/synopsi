package com.study.synopsi.repository;

import com.study.synopsi.model.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SourceRepository extends JpaRepository<Source, Long> {

    /**
     * Find source by name (case-insensitive)
     */
    Optional<Source> findByNameIgnoreCase(String name);

    /**
     * Find source by base URL
     */
    Optional<Source> findByBaseUrl(String baseUrl);

    /**
     * Find all active sources
     */
    List<Source> findByIsActive(Boolean isActive);

    /**
     * Find sources by type
     */
    List<Source> findBySourceType(Source.SourceType sourceType);

    /**
     * Find source by ID with feeds eagerly loaded
     */
    @Query("SELECT s FROM Source s LEFT JOIN FETCH s.feeds WHERE s.id = :id")
    Optional<Source> findByIdWithFeeds(@Param("id") Long id);

    /**
     * Find all sources with feeds eagerly loaded
     */
    @Query("SELECT DISTINCT s FROM Source s LEFT JOIN FETCH s.feeds")
    List<Source> findAllWithFeeds();

    /**
     * Find active sources with feeds eagerly loaded
     */
    @Query("SELECT DISTINCT s FROM Source s LEFT JOIN FETCH s.feeds WHERE s.isActive = :isActive")
    List<Source> findByIsActiveWithFeeds(@Param("isActive") Boolean isActive);

    /**
     * Check if source exists by name (for validation)
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Check if source exists by base URL (for validation)
     */
    boolean existsByBaseUrl(String baseUrl);
}