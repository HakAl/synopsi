package com.study.synopsi.service;

import com.study.synopsi.model.Topic;
import com.study.synopsi.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds predefined topics into the database on application startup.
 * Only runs if the topics table is empty.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TopicSeedService implements ApplicationRunner {

    private final TopicRepository topicRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Only seed if topics table is empty
        if (topicRepository.count() > 0) {
            log.info("Topics already exist in database. Skipping seed data.");
            return;
        }

        log.info("Seeding predefined topics into database...");
        
        List<Topic> topics = createPredefinedTopics();
        topicRepository.saveAll(topics);
        
        log.info("Successfully seeded {} topics into database", topics.size());
    }

    /**
     * Creates the predefined list of root topics for article categorization.
     * These topics are used by NLP workers to classify articles.
     */
    private List<Topic> createPredefinedTopics() {
        List<Topic> topics = new ArrayList<>();

        topics.add(createTopic(
            "Technology",
            "technology",
            "Technology news, gadgets, software, and innovation"
        ));

        topics.add(createTopic(
            "Business",
            "business",
            "Business news, finance, economy, and markets"
        ));

        topics.add(createTopic(
            "Science",
            "science",
            "Scientific research, discoveries, and breakthroughs"
        ));

        topics.add(createTopic(
            "Health",
            "health",
            "Health, medicine, wellness, and fitness"
        ));

        topics.add(createTopic(
            "Politics",
            "politics",
            "Political news, government, and policy"
        ));

        topics.add(createTopic(
            "Sports",
            "sports",
            "Sports news, games, athletes, and competitions"
        ));

        topics.add(createTopic(
            "Entertainment",
            "entertainment",
            "Movies, TV, music, celebrities, and pop culture"
        ));

        topics.add(createTopic(
            "World News",
            "world-news",
            "International news and global events"
        ));

        topics.add(createTopic(
            "Finance",
            "finance",
            "Personal finance, investing, and money management"
        ));

        topics.add(createTopic(
            "Education",
            "education",
            "Education news, learning, schools, and universities"
        ));

        topics.add(createTopic(
            "Environment",
            "environment",
            "Climate, sustainability, and environmental issues"
        ));

        topics.add(createTopic(
            "Lifestyle",
            "lifestyle",
            "Fashion, food, travel, and lifestyle content"
        ));

        topics.add(createTopic(
            "Opinion",
            "opinion",
            "Opinion pieces, editorials, and commentary"
        ));

        topics.add(createTopic(
            "Local News",
            "local-news",
            "Local and regional news coverage"
        ));

        topics.add(createTopic(
            "Other",
            "other",
            "Miscellaneous topics not fitting other categories"
        ));

        return topics;
    }

    /**
     * Helper method to create a topic entity
     */
    private Topic createTopic(String name, String slug, String description) {
        Topic topic = new Topic();
        topic.setName(name);
        topic.setSlug(slug);
        topic.setDescription(description);
        topic.setIsActive(true);
        topic.setParentTopic(null); // Root topic
        return topic;
    }
}