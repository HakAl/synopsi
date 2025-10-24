package com.study.synopsi.repository;

import com.study.synopsi.model.Feed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeedRepository extends JpaRepository<Feed, Long>, JpaSpecificationExecutor<Feed> {

    // Basic query methods
    List<Feed> findBySourceId(Long sourceId);

    List<Feed> findByIsActiveTrue();

    List<Feed> findByFeedType(Feed.FeedType feedType);

    List<Feed> findByPriorityBetween(Integer minPriority, Integer maxPriority);

    boolean existsByFeedUrl(String feedUrl);

    // Query for feeds that need crawling
    // Returns feeds where: isActive = true AND (lastCrawled is null OR lastCrawled + crawlFrequencyMinutes <= now)
    @Query("""
        SELECT f FROM Feed f
        WHERE f.isActive = true
        AND (f.lastCrawled IS NULL
            OR FUNCTION('TIMESTAMPADD', MINUTE, f.crawlFrequencyMinutes, f.lastCrawled) <= :now)
        ORDER BY f.priority DESC, f.lastCrawled ASC NULLS FIRST
        """)
    List<Feed> findFeedsNeedingCrawl(@Param("now") LocalDateTime now);

    // Query for feeds with failures
    List<Feed> findByFailureCountGreaterThan(Integer count);

    // Query for feeds by topic
    @Query("SELECT f FROM Feed f WHERE f.topic.id = :topicId")
    List<Feed> findByTopicId(@Param("topicId") Long topicId);

    // Count articles per feed
    @Query("SELECT COUNT(a) FROM Article a WHERE a.feed.id = :feedId")
    Integer countArticlesByFeedId(@Param("feedId") Long feedId);
}