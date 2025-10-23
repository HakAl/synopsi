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
@Table(name = "topics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(unique = true, length = 100)
    private String slug; // URL-friendly version of name

    // Self-referential relationship for hierarchical topics
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_topic_id")
    private Topic parentTopic;

    @OneToMany(mappedBy = "parentTopic", cascade = CascadeType.ALL)
    private Set<Topic> childTopics = new HashSet<>();

    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ArticleTopic> articleTopics = new HashSet<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserPreference> userPreferences = new HashSet<>();

    // Helper methods
    public void addChildTopic(Topic child) {
        childTopics.add(child);
        child.setParentTopic(this);
    }

    public void removeChildTopic(Topic child) {
        childTopics.remove(child);
        child.setParentTopic(null);
    }
}