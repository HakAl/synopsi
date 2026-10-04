package com.study.synopsi.service;

import com.study.synopsi.dto.ArticleStatsResponse;
import com.study.synopsi.model.Article;
import com.study.synopsi.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final ArticleRepository articleRepository;

    /**
     * Get article statistics including total count and count by status
     */
    @Transactional(readOnly = true)
    public ArticleStatsResponse getArticleStats() {
        long totalCount = articleRepository.count();

        Map<String, Long> countByStatus = new LinkedHashMap<>();
        Arrays.stream(Article.ArticleStatus.values()).forEach(status -> {
            Specification<Article> spec = (root, query, cb) -> cb.equal(root.get("status"), status);
            long count = articleRepository.count(spec);
            countByStatus.put(status.name(), count);
        });

        return ArticleStatsResponse.builder()
                .totalCount(totalCount)
                .countByStatus(countByStatus)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}
