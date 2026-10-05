package com.study.synopsi.controller;

import com.study.synopsi.config.AuthenticatedUser;
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
import com.study.synopsi.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The summarization worker reads jobs from /api/v1/summaries/jobs/queued and
 * then fetches each job's article by id. These tests go through the real
 * serializer with real persisted entities, deliberately without an enclosing
 * test transaction, because an open session would resolve the lazy
 * associations and hide the defect that the worker actually hits.
 * The queue route is reserved for the WORKER role and the single-job route
 * is owner-scoped (the job here is shared, so any caller may read it), so
 * requests carry a real AuthenticatedUser principal built from a persisted
 * WORKER account.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SummaryJobSerializationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private SummaryJobRepository summaryJobRepository;

    @Autowired
    private UserRepository userRepository;

    private Long articleId;
    private Long jobId;
    private User worker;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        User account = new User();
        account.setUsername("serialization-worker-" + System.nanoTime());
        account.setEmail(account.getUsername() + "@worker.synopsi.local");
        account.setPassword("not-used");
        account.setRole(User.UserRole.WORKER);
        account.setEnabled(true);
        account.setAccountLocked(false);
        worker = userRepository.save(account);

        Source source = new Source();
        source.setName("Serialization Test Source");
        source.setBaseUrl("https://serialization-test.example.com");
        source.setSourceType(Source.SourceType.NEWS);
        source.setIsActive(true);
        source.setCreatedAt(now);
        source.setUpdatedAt(now);
        source = sourceRepository.save(source);

        Feed feed = new Feed();
        feed.setSource(source);
        feed.setFeedUrl("https://serialization-test.example.com/rss");
        feed.setFeedType(Feed.FeedType.RSS);
        feed.setTitle("Serialization Test Feed");
        feed.setCrawlFrequencyMinutes(60);
        feed.setIsActive(true);
        feed.setPriority(5);
        feed.setFailureCount(0);
        feed.setCreatedAt(now);
        feed.setUpdatedAt(now);
        feed = feedRepository.save(feed);

        Article article = new Article();
        article.setFeed(feed);
        article.setTitle("An article awaiting summarization");
        article.setOriginalUrl("https://serialization-test.example.com/article-1");
        article.setContent("Body text the worker will summarize.");
        article.setPublicationDate(now);
        article.setStatus(Article.ArticleStatus.PENDING);
        article.setCreatedAt(now);
        article.setUpdatedAt(now);
        article = articleRepository.save(article);
        articleId = article.getId();

        SummaryJob job = new SummaryJob();
        job.setArticle(article);
        job.setSummaryType(Summary.SummaryType.BRIEF);
        job.setSummaryLength(Summary.SummaryLength.MEDIUM);
        job.setStatus(SummaryJob.JobStatus.QUEUED);
        job.setPriority(5);
        job.setAttempts(0);
        job.setMaxAttempts(3);
        job.setSubmittedAt(now);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        job = summaryJobRepository.save(job);
        jobId = job.getId();
    }

    @AfterEach
    void tearDown() {
        summaryJobRepository.deleteAll();
        articleRepository.deleteAll();
        feedRepository.deleteAll();
        sourceRepository.deleteAll();
        userRepository.delete(worker);
    }

    private MockHttpServletRequestBuilder asWorker(MockHttpServletRequestBuilder request) {
        AuthenticatedUser principal = new AuthenticatedUser(worker);
        return request.with(authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())));
    }

    @Test
    @DisplayName("Queued jobs serialize and expose the articleId the worker reads")
    void queuedJobsExposeArticleId() throws Exception {
        mockMvc.perform(asWorker(get("/api/v1/summaries/jobs/queued")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", equalTo(jobId.intValue())))
                .andExpect(jsonPath("$[0].articleId", equalTo(articleId.intValue())))
                .andExpect(jsonPath("$[0].status", equalTo("QUEUED")));
    }

    @Test
    @DisplayName("A single job serializes and exposes its articleId")
    void singleJobExposesArticleId() throws Exception {
        mockMvc.perform(asWorker(get("/api/v1/summaries/jobs/" + jobId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articleId", equalTo(articleId.intValue())));
    }
}
