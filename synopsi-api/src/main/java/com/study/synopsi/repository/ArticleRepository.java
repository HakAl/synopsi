package com.study.synopsi.repository;

import com.study.synopsi.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    // JpaRepository provides:
    // - findAll()
    // - findById(Long id)
    // - save(Article article)
    // - deleteById(Long id)
    // etc.

    // Add custom queries here if needed, for example:
    // List<Article> findByStatus(Article.ArticleStatus status);
    // List<Article> findByPublicationDateBetween(LocalDateTime start, LocalDateTime end);
}