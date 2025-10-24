package com.study.synopsi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sources")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(nullable = false, length = 500)
    private String baseUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private Double credibilityScore; // 0.0 to 1.0 or 0 to 100

    @Column(length = 100)
    private String language; // Primary language of the source

    @Column(length = 100)
    private String country; // Primary country/region

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private SourceType sourceType;

    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // One source can have many feeds
    @OneToMany(mappedBy = "source", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Feed> feeds = new HashSet<>();

    public enum SourceType {
        NEWS,
        BLOG,
        ACADEMIC,
        MAGAZINE,
        PODCAST,
        RSS,
        VIDEO,
        OTHER
    }

    // Helper methods
    public void addFeed(Feed feed) {
        feeds.add(feed);
        feed.setSource(this);
    }

    public void removeFeed(Feed feed) {
        feeds.remove(feed);
        feed.setSource(null);
    }
}