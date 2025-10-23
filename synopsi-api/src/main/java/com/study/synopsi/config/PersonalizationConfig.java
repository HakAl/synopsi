package com.study.synopsi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "synopsi.personalization")
@Getter
@Setter
public class PersonalizationConfig {
    
    // Scoring weights (should sum to 1.0)
    private ScoringWeights weights = new ScoringWeights();
    
    // Thresholds
    private Thresholds thresholds = new Thresholds();
    
    // Time decay parameters
    private TimeDecay timeDecay = new TimeDecay();
    
    // Cache settings
    private CacheSettings cache = new CacheSettings();
    
    @Getter
    @Setter
    public static class ScoringWeights {
        private double topicPreference = 0.40;    // User's explicit topic preferences
        private double readingHistory = 0.30;     // Reading time, completion rate
        private double positiveFeedback = 0.20;   // Likes, saves
        private double recency = 0.10;            // How recent the article is
    }
    
    @Getter
    @Setter
    public static class Thresholds {
        private int minCompletionForEngaged = 70;      // % completion to consider "engaged"
        private int minReadTimeForEngaged = 60;        // seconds to consider "engaged"
        private int highInterestThreshold = 3;         // HIGH/VERY_HIGH on 1-5 scale
        private double minRelevanceScore = 0.3;        // Min score to recommend
    }
    
    @Getter
    @Setter
    public static class TimeDecay {
        private int decayDays = 30;                    // Days before recency score starts decaying
        private double decayRate = 0.95;               // Daily decay multiplier
    }
    
    @Getter
    @Setter
    public static class CacheSettings {
        private int ttlMinutes = 10;                   // Cache TTL for personalized feeds
        private int maxCacheSize = 1000;               // Max users to cache
    }
}