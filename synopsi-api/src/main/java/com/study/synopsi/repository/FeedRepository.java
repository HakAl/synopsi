package com.study.synopsi.repository;

import com.study.synopsi.model.Feed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedRepository extends JpaRepository<Feed, Long> {
    // Basic CRUD operations provided by JpaRepository
}