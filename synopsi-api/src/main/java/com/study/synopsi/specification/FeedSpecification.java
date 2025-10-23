package com.study.synopsi.specification;

import com.study.synopsi.dto.filter.FeedFilterParams;
import com.study.synopsi.model.Feed;
import com.study.synopsi.model.Source;
import com.study.synopsi.model.Topic;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class FeedSpecification {

    /**
     * Build dynamic specification from filter parameters
     */
    public static Specification<Feed> buildSpecification(FeedFilterParams filters) {
        return (root, query, criteriaBuilder) -> {
            
            if (filters == null) {
                return criteriaBuilder.conjunction();
            }
            
            List<Predicate> predicates = new ArrayList<>();
            
            // Filter by source ID
            if (filters.getSourceId() != null) {
                Join<Feed, Source> sourceJoin = root.join("source");
                predicates.add(criteriaBuilder.equal(sourceJoin.get("id"), filters.getSourceId()));
            }
            
            // Filter by feed type
            if (filters.getFeedType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("feedType"), filters.getFeedType()));
            }
            
            // Filter by active status
            if (filters.getIsActive() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), filters.getIsActive()));
            }
            
            // Filter by topic ID
            if (filters.getTopicId() != null) {
                Join<Feed, Topic> topicJoin = root.join("topic");
                predicates.add(criteriaBuilder.equal(topicJoin.get("id"), filters.getTopicId()));
            }
            
            // Filter by priority range
            if (filters.getMinPriority() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("priority"), filters.getMinPriority()));
            }
            if (filters.getMaxPriority() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("priority"), filters.getMaxPriority()));
            }
            
            // Filter by last crawled date range
            if (filters.getLastCrawledAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("lastCrawled"), filters.getLastCrawledAfter()));
            }
            if (filters.getLastCrawledBefore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("lastCrawled"), filters.getLastCrawledBefore()));
            }
            
            // Filter by failure count range
            if (filters.getMinFailureCount() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("failureCount"), filters.getMinFailureCount()));
            }
            if (filters.getMaxFailureCount() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("failureCount"), filters.getMaxFailureCount()));
            }
            
            // Search in title, description, or URL
            if (filters.getSearchTerm() != null && !filters.getSearchTerm().trim().isEmpty()) {
                String searchPattern = "%" + filters.getSearchTerm().toLowerCase() + "%";
                Predicate titlePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("title")), searchPattern);
                Predicate descriptionPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")), searchPattern);
                Predicate urlPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("feedUrl")), searchPattern);
                
                predicates.add(criteriaBuilder.or(titlePredicate, descriptionPredicate, urlPredicate));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}