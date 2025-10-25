package com.study.synopsi.service;

import com.study.synopsi.config.PersonalizationConfig;
import com.study.synopsi.dto.*;
import com.study.synopsi.model.*;
import com.study.synopsi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalizationService {

    private final ArticleRepository articleRepository;
    private final ReadingHistoryRepository readingHistoryRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserArticleFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;

    private final PersonalizationConfig config;

    /**
     * Get personalized article recommendations for a user
     */
    @Cacheable(value = "personalizedFeed", key = "#userId + '-' + #pageable.pageNumber")
    @Transactional(readOnly = true)
    public Page<PersonalizedArticleDto> getPersonalizedArticles(Long userId, Pageable pageable) {
        log.info("Generating personalized feed for user: {}", userId);

        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Get all published articles
        List<Article> articles = articleRepository.findAll();

        // Get user's interaction data
        Map<Long, ReadingHistory> readingHistoryMap = getReadingHistoryMap(userId);
        Map<Long, List<UserArticleFeedback>> feedbackMap = getFeedbackMap(userId);
        List<UserPreference> preferences = userPreferenceRepository.findByUserIdAndIsActiveTrue(userId);

        // Score and sort articles
        List<PersonalizedArticleDto> scoredArticles = articles.stream()
                .map(article -> scoreArticle(article, userId, readingHistoryMap, feedbackMap, preferences))
                .filter(dto -> dto.getRelevanceScore() >= config.getThresholds().getMinRelevanceScore())
                .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                .collect(Collectors.toList());

        // Paginate results
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), scoredArticles.size());

        if (start > scoredArticles.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, scoredArticles.size());
        }

        List<PersonalizedArticleDto> pageContent = scoredArticles.subList(start, end);
        return new PageImpl<>(pageContent, pageable, scoredArticles.size());
    }

    /**
     * Calculate relevance score for an article
     */
    private PersonalizedArticleDto scoreArticle(
            Article article,
            Long userId,
            Map<Long, ReadingHistory> readingHistoryMap,
            Map<Long, List<UserArticleFeedback>> feedbackMap,
            List<UserPreference> preferences) {

        double topicScore = calculateTopicScore(article, preferences);
        double readingScore = calculateReadingScore(article.getId(), readingHistoryMap);
        double feedbackScore = calculateFeedbackScore(article.getId(), feedbackMap);
        double recencyScore = calculateRecencyScore(article.getPublicationDate());

        // Weighted combination
        double relevanceScore =
                (topicScore * config.getWeights().getTopicPreference()) +
                        (readingScore * config.getWeights().getReadingHistory()) +
                        (feedbackScore * config.getWeights().getPositiveFeedback()) +
                        (recencyScore * config.getWeights().getRecency());

        String reason = generateRecommendationReason(topicScore, readingScore, feedbackScore, recencyScore);

        return buildPersonalizedArticleDto(article, userId, relevanceScore, reason,
                readingHistoryMap, feedbackMap);
    }

    /**
     * Calculate topic preference score
     */
    private double calculateTopicScore(Article article, List<UserPreference> preferences) {
        if (preferences.isEmpty() || article.getArticleTopics().isEmpty()) {
            return 0.5; // Neutral score if no preferences
        }

        Map<Long, UserPreference.InterestLevel> preferenceMap = preferences.stream()
                .collect(Collectors.toMap(
                        p -> p.getTopic().getId(),
                        UserPreference::getInterestLevel
                ));

        double totalScore = 0.0;
        int matchCount = 0;

        for (ArticleTopic at : article.getArticleTopics()) {
            Long topicId = at.getTopic().getId();
            if (preferenceMap.containsKey(topicId)) {
                totalScore += interestLevelToScore(preferenceMap.get(topicId));
                matchCount++;
            }
        }

        return matchCount > 0 ? totalScore / matchCount : 0.5;
    }

    /**
     * Calculate reading history score
     */
    private double calculateReadingScore(Long articleId, Map<Long, ReadingHistory> historyMap) {
        ReadingHistory history = historyMap.get(articleId);
        if (history == null) {
            return 0.5; // Neutral for unread articles
        }

        double score = 0.0;
        int factors = 0;

        // High completion rate is positive
        if (history.getCompletionPercentage() != null) {
            score += Math.min(history.getCompletionPercentage() / 100.0, 1.0);
            factors++;
        }

        // Significant time spent is positive
        if (history.getTimeSpentSeconds() != null) {
            int threshold = config.getThresholds().getMinReadTimeForEngaged();
            score += Math.min((double) history.getTimeSpentSeconds() / (threshold * 2), 1.0);
            factors++;
        }

        // Multiple reads indicate high interest
        if (history.getAccessCount() > 1) {
            score += Math.min(history.getAccessCount() / 5.0, 1.0);
            factors++;
        }

        return factors > 0 ? score / factors : 0.5;
    }

    /**
     * Calculate feedback score
     */
    private double calculateFeedbackScore(Long articleId, Map<Long, List<UserArticleFeedback>> feedbackMap) {
        List<UserArticleFeedback> feedbacks = feedbackMap.get(articleId);
        if (feedbacks == null || feedbacks.isEmpty()) {
            return 0.5; // Neutral for no feedback
        }

        double score = 0.5;

        for (UserArticleFeedback feedback : feedbacks) {
            switch (feedback.getFeedbackType()) {
                case LIKED -> score += 0.3;
                case SAVED -> score += 0.4;
                case SHARED -> score += 0.2;
                case DISLIKED -> score -= 0.5;
                case REPORTED -> score -= 1.0;
                case ARCHIVED -> score -= 0.1;
            }

            // Consider rating if present
            if (feedback.getRating() != null) {
                score += (feedback.getRating() - 3) * 0.1; // -0.2 to +0.2
            }
        }

        return Math.max(0.0, Math.min(1.0, score));
    }

    /**
     * Calculate recency score with time decay
     */
    private double calculateRecencyScore(LocalDateTime publicationDate) {
        if (publicationDate == null) {
            return 0.5;
        }

        long daysOld = ChronoUnit.DAYS.between(publicationDate, LocalDateTime.now());

        if (daysOld < 0) {
            return 1.0; // Future articles (edge case)
        }

        if (daysOld <= config.getTimeDecay().getDecayDays()) {
            return 1.0; // Fresh content
        }

        // Apply exponential decay
        long decayPeriod = daysOld - config.getTimeDecay().getDecayDays();
        return Math.max(0.0, Math.pow(config.getTimeDecay().getDecayRate(), decayPeriod));
    }

    /**
     * Generate human-readable recommendation reason
     */
    private String generateRecommendationReason(double topicScore, double readingScore,
                                                double feedbackScore, double recencyScore) {
        List<String> reasons = new ArrayList<>();

        if (topicScore > 0.7) {
            reasons.add("matches your interests");
        }
        if (readingScore > 0.7) {
            reasons.add("similar to articles you've engaged with");
        }
        if (feedbackScore > 0.7) {
            reasons.add("based on your positive feedback");
        }
        if (recencyScore > 0.9) {
            reasons.add("newly published");
        }

        if (reasons.isEmpty()) {
            return "recommended for you";
        }

        return "Recommended: " + String.join(", ", reasons);
    }

    /**
     * Convert interest level to numeric score
     */
    private double interestLevelToScore(UserPreference.InterestLevel level) {
        return switch (level) {
            case VERY_LOW -> 0.2;
            case LOW -> 0.4;
            case MEDIUM -> 0.6;
            case HIGH -> 0.8;
            case VERY_HIGH -> 1.0;
        };
    }

    /**
     * Record user reading interaction
     */
    @Transactional
    @CacheEvict(value = "personalizedFeed", key = "#userId + '-*'")
    public void recordReadingInteraction(Long userId, ArticleInteractionDto interaction) {
        log.info("Recording reading interaction for user {} on article {}",
                userId, interaction.getArticleId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Article article = articleRepository.findById(interaction.getArticleId())
                .orElseThrow(() -> new RuntimeException("Article not found: " + interaction.getArticleId()));

        Optional<ReadingHistory> existingHistory =
                readingHistoryRepository.findByUserIdAndArticleId(userId, interaction.getArticleId());

        if (existingHistory.isPresent()) {
            // Update existing record
            ReadingHistory history = existingHistory.get();
            history.recordReRead(interaction.getTimeSpentSeconds(), interaction.getCompletionPercentage());
            readingHistoryRepository.save(history);
        } else {
            // Create new record
            ReadingHistory history = new ReadingHistory();
            history.setUser(user);
            history.setArticle(article);
            history.setReadAt(LocalDateTime.now());
            history.setTimeSpentSeconds(interaction.getTimeSpentSeconds());
            history.setCompletionPercentage(interaction.getCompletionPercentage());
            history.setLastAccessedAt(LocalDateTime.now());
            history.setAccessCount(1);
            readingHistoryRepository.save(history);
        }
    }

    /**
     * Record user feedback (like, save, etc.)
     */
    @Transactional
    @CacheEvict(value = "personalizedFeed", key = "#userId + '-*'")
    public void recordFeedback(Long userId, ArticleInteractionDto interaction) {
        log.info("Recording feedback for user {} on article {}: {}",
                userId, interaction.getArticleId(), interaction.getFeedbackType());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Article article = articleRepository.findById(interaction.getArticleId())
                .orElseThrow(() -> new RuntimeException("Article not found: " + interaction.getArticleId()));

        Optional<UserArticleFeedback> existing = feedbackRepository
                .findByUserIdAndArticleIdAndFeedbackType(userId, interaction.getArticleId(),
                        interaction.getFeedbackType());

        if (existing.isPresent()) {
            // Update existing feedback
            UserArticleFeedback feedback = existing.get();
            if (interaction.getRating() != null) {
                feedback.setRating(interaction.getRating());
            }
            if (interaction.getComment() != null) {
                feedback.setComment(interaction.getComment());
            }
            feedbackRepository.save(feedback);
        } else {
            // Create new feedback
            UserArticleFeedback feedback = new UserArticleFeedback();
            feedback.setUser(user);
            feedback.setArticle(article);
            feedback.setFeedbackType(interaction.getFeedbackType());
            feedback.setRating(interaction.getRating());
            feedback.setComment(interaction.getComment());
            feedbackRepository.save(feedback);
        }
    }

    /**
     * Get user's topic preferences
     */
    @Transactional(readOnly = true)
    public List<UserPreferenceDto> getUserPreferences(Long userId) {
        List<UserPreference> preferences = userPreferenceRepository.findByUserId(userId);
        return preferences.stream()
                .map(this::toUserPreferenceDto)
                .collect(Collectors.toList());
    }

    /**
     * Update user preference
     */
    @Transactional
    @CacheEvict(value = "personalizedFeed", key = "#userId + '-*'")
    public UserPreferenceDto updateUserPreference(Long userId, UserPreferenceDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Optional<UserPreference> existing =
                userPreferenceRepository.findByUserIdAndTopicId(userId, dto.getTopicId());

        UserPreference preference;
        if (existing.isPresent()) {
            preference = existing.get();

            // Use default MEDIUM if not provided
            preference.setInterestLevel(dto.getInterestLevel() != null ?
                    dto.getInterestLevel() : UserPreference.InterestLevel.MEDIUM);
            preference.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        } else {
            // Fetch the Topic entity
            Topic topic = topicRepository.findById(dto.getTopicId())
                    .orElseThrow(() -> new RuntimeException("Topic not found: " + dto.getTopicId()));

            preference = new UserPreference();
            preference.setUser(user);
            preference.setTopic(topic);

            // Use default MEDIUM if not provided
            preference.setInterestLevel(dto.getInterestLevel() != null ?
                    dto.getInterestLevel() : UserPreference.InterestLevel.MEDIUM);
            preference.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        }

        preference = userPreferenceRepository.save(preference);
        return toUserPreferenceDto(preference);
    }

    /**
     * Get user's inferred interests based on behavior
     */
    @Transactional(readOnly = true)
    public List<UserTopicInterestDto> getInferredInterests(Long userId) {
        // Get explicit preferences
        List<UserPreference> preferences = userPreferenceRepository.findByUserIdAndIsActiveTrue(userId);
        Map<Long, UserPreference> preferenceMap = preferences.stream()
                .collect(Collectors.toMap(p -> p.getTopic().getId(), p -> p));

        // Get behavioral data
        List<Object[]> topicReadData = readingHistoryRepository.getMostReadTopicsByUser(userId);

        return topicReadData.stream()
                .map(data -> {
                    Long topicId = (Long) data[0];
                    String topicName = (String) data[1];
                    Long readCount = (Long) data[2];

                    UserTopicInterestDto dto = new UserTopicInterestDto();
                    dto.setTopicId(topicId);
                    dto.setTopicName(topicName);
                    dto.setArticlesRead(readCount);

                    // Add explicit preference if exists
                    if (preferenceMap.containsKey(topicId)) {
                        dto.setExplicitInterestLevel(preferenceMap.get(topicId).getInterestLevel().toString());
                    }

                    // Calculate inferred score (simple heuristic for now)
                    double inferredScore = Math.min(1.0, readCount / 20.0);
                    dto.setInferredInterestScore(inferredScore);

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get similar articles based on user's reading history
     */
    @Transactional(readOnly = true)
    public List<PersonalizedArticleDto> getSimilarArticles(Long userId, Long articleId, int limit) {
        // For now, find articles with similar topics
        Article sourceArticle = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found: " + articleId));

        Set<Long> sourceTopicIds = sourceArticle.getArticleTopics().stream()
                .map(at -> at.getTopic().getId())
                .collect(Collectors.toSet());

        if (sourceTopicIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Get user context
        Map<Long, ReadingHistory> readingHistoryMap = getReadingHistoryMap(userId);
        Map<Long, List<UserArticleFeedback>> feedbackMap = getFeedbackMap(userId);
        List<UserPreference> preferences = userPreferenceRepository.findByUserIdAndIsActiveTrue(userId);

        // Find articles with overlapping topics
        List<Article> allArticles = articleRepository.findAll();

        return allArticles.stream()
                .filter(a -> !a.getId().equals(articleId)) // Exclude source article
                .filter(a -> hasTopicOverlap(a, sourceTopicIds))
                .map(a -> scoreArticle(a, userId, readingHistoryMap, feedbackMap, preferences))
                .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Helper methods

    private Map<Long, ReadingHistory> getReadingHistoryMap(Long userId) {
        List<ReadingHistory> histories = readingHistoryRepository.findByUserId(userId, Pageable.unpaged()).getContent();
        return histories.stream()
                .collect(Collectors.toMap(h -> h.getArticle().getId(), h -> h));
    }

    private Map<Long, List<UserArticleFeedback>> getFeedbackMap(Long userId) {
        List<UserArticleFeedback> feedbacks = feedbackRepository.findByUserId(userId, Pageable.unpaged()).getContent();
        return feedbacks.stream()
                .collect(Collectors.groupingBy(f -> f.getArticle().getId()));
    }

    private boolean hasTopicOverlap(Article article, Set<Long> topicIds) {
        return article.getArticleTopics().stream()
                .anyMatch(at -> topicIds.contains(at.getTopic().getId()));
    }

    private PersonalizedArticleDto buildPersonalizedArticleDto(
            Article article,
            Long userId,
            double relevanceScore,
            String reason,
            Map<Long, ReadingHistory> readingHistoryMap,
            Map<Long, List<UserArticleFeedback>> feedbackMap) {

        ReadingHistory history = readingHistoryMap.get(article.getId());
        List<UserArticleFeedback> feedbacks = feedbackMap.get(article.getId());

        // Get default summary if available
        String summaryText = null;
        if (article.getSummaries() != null && !article.getSummaries().isEmpty()) {
            summaryText = article.getSummaries().stream()
                    .filter(s -> s.getUser() == null) // Default summary
                    .filter(s -> s.getStatus() == Summary.SummaryStatus.COMPLETED)
                    .findFirst()
                    .map(Summary::getSummaryText)
                    .orElse(null);
        }

        PersonalizedArticleDto dto = PersonalizedArticleDto.builder()
                .articleId(article.getId())
                .title(article.getTitle())
                .originalUrl(article.getOriginalUrl())
                .summary(summaryText)
                .publicationDate(article.getPublicationDate())
                .author(article.getAuthor())
                .imageUrl(article.getImageUrl())
                .description(article.getDescription())
                .readTimeMinutes(article.getReadTimeMinutes())
                .language(article.getLanguage())
                .feedTitle(article.getFeed() != null ? article.getFeed().getTitle() : null)
                .sourceName(article.getFeed() != null && article.getFeed().getSource() != null ?
                        article.getFeed().getSource().getName() : null)
                .topicNames(article.getArticleTopics().stream()
                        .map(at -> at.getTopic().getName())
                        .collect(Collectors.toList()))
                .topicIds(article.getArticleTopics().stream()
                        .map(at -> at.getTopic().getId())
                        .collect(Collectors.toList()))
                .relevanceScore(relevanceScore)
                .recommendationReason(reason)
                .hasRead(history != null)
                .build();

        // Add reading history details
        if (history != null) {
            dto.setReadCount(history.getAccessCount());
            dto.setCompletionPercentage(history.getCompletionPercentage());
            dto.setLastReadAt(history.getLastAccessedAt());
        }

        // Add feedback details
        if (feedbacks != null) {
            for (UserArticleFeedback feedback : feedbacks) {
                switch (feedback.getFeedbackType()) {
                    case LIKED -> dto.setIsLiked(true);
                    case SAVED -> dto.setIsSaved(true);
                    case DISLIKED -> dto.setIsDisliked(true);
                }
                if (feedback.getRating() != null) {
                    dto.setUserRating(feedback.getRating());
                }
            }
        }

        return dto;
    }

    private UserPreferenceDto toUserPreferenceDto(UserPreference preference) {
        return UserPreferenceDto.builder()
                .id(preference.getId())
                .topicId(preference.getTopic().getId())
                .topicName(preference.getTopic().getName())
                .interestLevel(preference.getInterestLevel())
                .isActive(preference.getIsActive())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }
}
