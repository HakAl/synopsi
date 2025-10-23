package com.study.synopsi.specification;

import com.study.synopsi.dto.filter.ArticleFilterParams;
import com.study.synopsi.model.Article;
import com.study.synopsi.model.Article.ArticleStatus;
import com.study.synopsi.model.Feed;
import com.study.synopsi.model.Source;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Specifications for building dynamic Article queries
 * Uses JPA Criteria API for type-safe, composable queries
 */
public class ArticleSpecification {

    /**
     * Filter by article status
     */
    public static Specification<Article> hasStatus(ArticleStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction(); // No filter
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    /**
     * Filter by feed ID
     */
    public static Specification<Article> hasFeedId(Long feedId) {
        return (root, query, criteriaBuilder) -> {
            if (feedId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("feed").get("id"), feedId);
        };
    }

    /**
     * Filter by source name (joins through feed -> source)
     */
    public static Specification<Article> hasSourceName(String sourceName) {
        return (root, query, criteriaBuilder) -> {
            if (sourceName == null || sourceName.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            // Join article -> feed -> source
            Join<Article, Feed> feedJoin = root.join("feed");
            Join<Feed, Source> sourceJoin = feedJoin.join("source");

            // Case-insensitive match
            return criteriaBuilder.like(
                    criteriaBuilder.lower(sourceJoin.get("name")),
                    "%" + sourceName.toLowerCase() + "%"
            );
        };
    }

    /**
     * Filter articles published on or after the given date
     */
    public static Specification<Article> publishedAfter(LocalDateTime startDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("publicationDate"), startDate);
        };
    }

    /**
     * Filter articles published on or before the given date
     */
    public static Specification<Article> publishedBefore(LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            if (endDate == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("publicationDate"), endDate);
        };
    }

    /**
     * Filter by language
     */
    public static Specification<Article> hasLanguage(String language) {
        return (root, query, criteriaBuilder) -> {
            if (language == null || language.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("language")),
                    language.toLowerCase()
            );
        };
    }

    /**
     * Full-text search across title, content, and description
     * Case-insensitive LIKE search
     */
    public static Specification<Article> searchByTerm(String searchTerm) {
        return (root, query, criteriaBuilder) -> {
            if (searchTerm == null || searchTerm.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String likePattern = "%" + searchTerm.toLowerCase() + "%";

            // Search in title, content, or description
            Predicate titleMatch = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("title")),
                    likePattern
            );

            Predicate contentMatch = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("content")),
                    likePattern
            );

            Predicate descriptionMatch = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")),
                    likePattern
            );

            return criteriaBuilder.or(titleMatch, contentMatch, descriptionMatch);
        };
    }

    /**
     * Build complete specification from filter parameters
     * Combines all active filters with AND logic
     */
    public static Specification<Article> buildSpecification(ArticleFilterParams params) {
        if (params == null) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction(); // No filters
        }

        return hasStatus(params.getStatus())
                .and(hasFeedId(params.getFeedId()))
                .and(hasSourceName(params.getSource()))
                .and(publishedAfter(params.getStartDate()))
                .and(publishedBefore(params.getEndDate()))
                .and(hasLanguage(params.getLanguage()))
                .and(searchByTerm(params.getSearchTerm()));
    }
}