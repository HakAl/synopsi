package com.study.synopsi.exception;

import com.study.synopsi.model.Article;
import com.study.synopsi.model.Feed;
import com.study.synopsi.model.Source;
import com.study.synopsi.repository.ArticleRepository;
import com.study.synopsi.repository.FeedRepository;
import com.study.synopsi.repository.SourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Proves the integrity-violation classification against the real H2 driver,
 * not a hand-built exception: the handler must say 409 for a genuine unique
 * violation and 400 for a length error even when the rejected value, which H2
 * echoes in its message, starts with "Unique".
 */
@SpringBootTest
@Transactional
@DisplayName("DataIntegrityViolation classification against the real driver")
class DataIntegrityClassificationIntegrationTest {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private GlobalExceptionHandler handler;

    private Feed feed;

    @BeforeEach
    void setUp() {
        Source source = new Source();
        source.setName("classification-test-" + System.nanoTime());
        source.setBaseUrl("https://classification.test");
        source.setIsActive(true);
        source = sourceRepository.saveAndFlush(source);

        feed = new Feed();
        feed.setSource(source);
        feed.setFeedUrl("https://classification.test/feed-" + System.nanoTime());
        feed.setFeedType(Feed.FeedType.RSS);
        feed.setTitle("classification test feed");
        feed = feedRepository.saveAndFlush(feed);
    }

    private Article article(String title, String url) {
        Article a = new Article();
        a.setTitle(title);
        a.setOriginalUrl(url);
        a.setContent("content");
        a.setPublicationDate(LocalDateTime.now());
        a.setFeed(feed);
        return a;
    }

    @Test
    @DisplayName("A real unique violation on originalUrl is a 409")
    void realUniqueViolationIsConflict() {
        String url = "https://classification.test/same-" + System.nanoTime();
        articleRepository.saveAndFlush(article("first", url));

        DataIntegrityViolationException ex = catchThrowableOfType(
                () -> articleRepository.saveAndFlush(article("second", url)),
                DataIntegrityViolationException.class);

        assertThat(ex).as("H2 should reject the duplicate originalUrl").isNotNull();
        ResponseEntity<?> response = handler.handleDataIntegrityViolation(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("A real length error whose rejected value starts with Unique is a 400, not a duplicate")
    void realLengthErrorStartingWithUniqueIsBadRequest() {
        String overlongTitle = "Unique duplicate key " + "x".repeat(500);

        DataIntegrityViolationException ex = catchThrowableOfType(
                () -> articleRepository.saveAndFlush(
                        article(overlongTitle, "https://classification.test/long-" + System.nanoTime())),
                DataIntegrityViolationException.class);

        assertThat(ex).as("H2 should reject a title longer than the 500-char column").isNotNull();
        assertThat(ex.getMostSpecificCause().getMessage())
                .as("precondition: the driver echoes the rejected value in its message")
                .containsIgnoringCase("unique");
        ResponseEntity<?> response = handler.handleDataIntegrityViolation(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
