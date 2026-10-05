package com.study.synopsi;

import com.study.synopsi.exception.GlobalExceptionHandler;
import com.study.synopsi.model.Article;
import com.study.synopsi.model.Feed;
import com.study.synopsi.model.Source;
import com.study.synopsi.model.Summary;
import com.study.synopsi.model.SummaryJob;
import com.study.synopsi.model.User;
import com.study.synopsi.repository.ArticleRepository;
import com.study.synopsi.repository.FeedRepository;
import com.study.synopsi.repository.SourceRepository;
import com.study.synopsi.repository.SummaryJobRepository;
import com.study.synopsi.repository.SummaryRepository;
import com.study.synopsi.repository.TopicRepository;
import com.study.synopsi.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Proves the Flyway migration matches the entities on a real PostgreSQL.
 * <p>
 * The context starts with the {@code postgres} profile, so Flyway applies
 * {@code db/migration/V1__initial_schema.sql} and Hibernate runs
 * {@code ddl-auto=validate} against the result; a missing table or column, or
 * a column whose type name differs, fails the context before any test method
 * runs. Validation compares type names only: it does not notice a
 * {@code varchar} length, a constraint or an index that differs from the
 * entity. The methods exercise part of that gap: inserts into the six tables
 * the workers and the auth flow write, the unique constraint the 409
 * classification depends on, the 500-character title limit the ingestion
 * worker relies on, and the startup seeder.
 * <p>
 * No test transaction: PostgreSQL aborts a transaction on the first integrity
 * error and rejects every later statement in it (SQLSTATE 25P02), unlike H2,
 * so each method stands alone and every write is flushed where it happens.
 * Skipped, not failed, when no Docker daemon is reachable; the API CI
 * workflow checks the JUnit XML so a skip cannot pass unnoticed there.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        // The profile deliberately has no default password; the container's
        // connection details supersede all three datasource values anyway.
        "spring.datasource.password=unused"
})
@ActiveProfiles("postgres")
@DisplayName("Flyway schema against a real PostgreSQL")
class PostgresSchemaMigrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SummaryRepository summaryRepository;

    @Autowired
    private SummaryJobRepository summaryJobRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private GlobalExceptionHandler handler;

    @Test
    @DisplayName("Entities round-trip through the migrated schema")
    void entitiesRoundTrip() {
        String tag = "roundtrip-" + System.nanoTime();
        LocalDateTime now = LocalDateTime.now();

        Source source = new Source();
        source.setName("source-" + tag);
        source.setBaseUrl("https://" + tag + ".example.com");
        source.setSourceType(Source.SourceType.NEWS);
        source = sourceRepository.saveAndFlush(source);

        Feed feed = new Feed();
        feed.setSource(source);
        feed.setFeedUrl("https://" + tag + ".example.com/feed");
        feed.setFeedType(Feed.FeedType.RSS);
        feed.setTitle("feed " + tag);
        feed = feedRepository.saveAndFlush(feed);

        Article article = new Article();
        article.setTitle("article " + tag);
        article.setOriginalUrl("https://" + tag + ".example.com/article");
        article.setContent("content " + tag);
        article.setPublicationDate(now);
        article.setFeed(feed);
        article = articleRepository.saveAndFlush(article);

        User user = new User();
        user.setUsername("user-" + tag);
        user.setEmail(tag + "@example.com");
        user.setPassword("not-used");
        user.setRole(User.UserRole.USER);
        user = userRepository.saveAndFlush(user);

        Summary summary = new Summary();
        summary.setArticle(article);
        summary.setUser(user);
        summary.setSummaryText("summary " + tag);
        summary.setGeneratedAt(now);
        summary = summaryRepository.saveAndFlush(summary);

        SummaryJob job = new SummaryJob();
        job.setArticle(article);
        job.setUser(user);
        job.setSubmittedAt(now);
        job = summaryJobRepository.saveAndFlush(job);

        assertThat(sourceRepository.findById(source.getId())).get()
                .extracting(Source::getName).isEqualTo("source-" + tag);
        assertThat(feedRepository.findById(feed.getId())).get()
                .extracting(Feed::getFeedUrl).isEqualTo("https://" + tag + ".example.com/feed");
        assertThat(articleRepository.findById(article.getId())).get()
                .extracting(Article::getStatus).isEqualTo(Article.ArticleStatus.PENDING);
        assertThat(userRepository.findById(user.getId())).get()
                .extracting(User::getRole).isEqualTo(User.UserRole.USER);
        assertThat(summaryRepository.findById(summary.getId())).get()
                .extracting(Summary::getStatus).isEqualTo(Summary.SummaryStatus.COMPLETED);
        assertThat(summaryJobRepository.findById(job.getId())).get()
                .extracting(SummaryJob::getStatus).isEqualTo(SummaryJob.JobStatus.QUEUED);
    }

    @Test
    @DisplayName("A duplicate originalUrl is SQLSTATE 23505 and classified as 409")
    void duplicateOriginalUrlIsUniqueViolation() {
        String tag = "duplicate-" + System.nanoTime();

        Source source = new Source();
        source.setName("source-" + tag);
        source.setBaseUrl("https://" + tag + ".example.com");
        source = sourceRepository.saveAndFlush(source);

        Feed unsaved = new Feed();
        unsaved.setSource(source);
        unsaved.setFeedUrl("https://" + tag + ".example.com/feed");
        unsaved.setFeedType(Feed.FeedType.RSS);
        Feed feed = feedRepository.saveAndFlush(unsaved);

        String url = "https://" + tag + ".example.com/same";
        articleRepository.saveAndFlush(article(feed, "first " + tag, url));

        DataIntegrityViolationException ex = catchThrowableOfType(
                () -> articleRepository.saveAndFlush(article(feed, "second " + tag, url)),
                DataIntegrityViolationException.class);

        assertThat(ex).as("PostgreSQL should reject the duplicate originalUrl").isNotNull();
        assertThat(firstSqlState(ex)).isEqualTo("23505");
        ResponseEntity<?> response = handler.handleDataIntegrityViolation(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("An overlong title is SQLSTATE 22001 and classified as 400, not a duplicate")
    void overlongTitleIsLengthViolation() {
        String tag = "overlong-" + System.nanoTime();

        Source source = new Source();
        source.setName("source-" + tag);
        source.setBaseUrl("https://" + tag + ".example.com");
        source = sourceRepository.saveAndFlush(source);

        Feed unsaved = new Feed();
        unsaved.setSource(source);
        unsaved.setFeedUrl("https://" + tag + ".example.com/feed");
        unsaved.setFeedType(Feed.FeedType.RSS);
        Feed feed = feedRepository.saveAndFlush(unsaved);

        String maxTitle = "x".repeat(500);
        assertThat(articleRepository.saveAndFlush(article(feed, maxTitle, "https://" + tag + ".example.com/max")).getId())
                .as("a 500-character title fits the declared column").isNotNull();

        String overlongTitle = "Unique " + "x".repeat(500);
        DataIntegrityViolationException ex = catchThrowableOfType(
                () -> articleRepository.saveAndFlush(article(feed, overlongTitle, "https://" + tag + ".example.com/long")),
                DataIntegrityViolationException.class);

        assertThat(ex).as("PostgreSQL should reject a title longer than varchar(500)").isNotNull();
        assertThat(firstSqlState(ex)).isEqualTo("22001");
        ResponseEntity<?> response = handler.handleDataIntegrityViolation(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("The topic seeder ran against the migrated schema")
    void topicSeederRan() {
        assertThat(topicRepository.count()).isPositive();
    }

    private static Article article(Feed feed, String title, String url) {
        Article a = new Article();
        a.setTitle(title);
        a.setOriginalUrl(url);
        a.setContent("content");
        a.setPublicationDate(LocalDateTime.now());
        a.setFeed(feed);
        return a;
    }

    private static String firstSqlState(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof SQLException sql) {
                return sql.getSQLState();
            }
            if (c.getCause() == c) {
                break;
            }
        }
        return null;
    }
}
