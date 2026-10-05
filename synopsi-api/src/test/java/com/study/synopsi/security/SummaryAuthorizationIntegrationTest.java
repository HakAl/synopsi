package com.study.synopsi.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.synopsi.dto.UserRequestDto;
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
import com.study.synopsi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Summaries and jobs are user-scoped even when the route carries only the
 * resource id. Policy: a resource you may not access does not exist for you,
 * so a foreign summary or job is a 404 exactly like a missing one and the id
 * space is not an existence oracle. Shared (user-less) resources are readable
 * by everyone; mutating them is reserved for the worker and admins.
 * Before this test existed Alice could read Bob's personalized summaries by
 * id or through the article-wide listing and could retry or regenerate his
 * jobs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Summaries and jobs addressed by id are owner-scoped")
class SummaryAuthorizationIntegrationTest {

    private static final String PASSWORD = "password123";
    private static final String FORBIDDEN_MESSAGE = "You don't have permission to access this resource.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private SummaryRepository summaryRepository;

    @Autowired
    private SummaryJobRepository summaryJobRepository;

    private record Account(Long id, String token) {}

    private Account alice;
    private Account bob;
    private Account admin;
    private Article article;
    private Summary aliceSummary;
    private Summary bobSummary;
    private Summary sharedSummary;
    private SummaryJob aliceJob;
    private SummaryJob bobJob;
    private SummaryJob sharedJob;
    private SummaryJob bobFailedJob;
    private SummaryJob sharedFailedJob;

    @BeforeEach
    void setUp() throws Exception {
        alice = register(unique("alice"));
        bob = register(unique("bob"));
        admin = registerAdmin(unique("admin"));
        article = createArticle();

        User aliceRow = userRepository.findById(alice.id()).orElseThrow();
        User bobRow = userRepository.findById(bob.id()).orElseThrow();

        aliceSummary = summary(aliceRow, "Alice's personalized summary");
        bobSummary = summary(bobRow, "Bob's personalized summary");
        sharedSummary = summary(null, "The default summary");

        aliceJob = job(aliceRow, SummaryJob.JobStatus.COMPLETED);
        bobJob = job(bobRow, SummaryJob.JobStatus.COMPLETED);
        sharedJob = job(null, SummaryJob.JobStatus.COMPLETED);

        bobFailedJob = new SummaryJob();
        bobFailedJob.setArticle(article);
        bobFailedJob.setUser(bobRow);
        bobFailedJob.setSummaryType(Summary.SummaryType.DETAILED);
        bobFailedJob.setStatus(SummaryJob.JobStatus.FAILED);
        bobFailedJob.setAttempts(1);
        bobFailedJob.setMaxAttempts(3);
        bobFailedJob.setErrorMessage("worker failed");
        bobFailedJob.setSubmittedAt(LocalDateTime.now());
        bobFailedJob = summaryJobRepository.saveAndFlush(bobFailedJob);

        sharedFailedJob = new SummaryJob();
        sharedFailedJob.setArticle(article);
        sharedFailedJob.setUser(null);
        sharedFailedJob.setSummaryType(Summary.SummaryType.DETAILED);
        sharedFailedJob.setStatus(SummaryJob.JobStatus.FAILED);
        sharedFailedJob.setAttempts(1);
        sharedFailedJob.setMaxAttempts(3);
        sharedFailedJob.setErrorMessage("worker failed");
        sharedFailedJob.setSubmittedAt(LocalDateTime.now());
        sharedFailedJob = summaryJobRepository.saveAndFlush(sharedFailedJob);
    }

    private Account register(String suffix) throws Exception {
        UserRequestDto request = UserRequestDto.builder()
                .username("sauthz-" + suffix)
                .email("sauthz-" + suffix + "@example.com")
                .password(PASSWORD)
                .build();
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return new Account(json.get("userId").asLong(), json.get("token").asText());
    }

    private Account registerAdmin(String suffix) throws Exception {
        Account account = register(suffix);
        User user = userRepository.findById(account.id()).orElseThrow();
        user.setRole(User.UserRole.ADMIN);
        userRepository.saveAndFlush(user);
        return account;
    }

    private Article createArticle() {
        Source source = new Source();
        source.setName("sauthz-source-" + System.nanoTime());
        source.setBaseUrl("https://sauthz.test");
        source.setIsActive(true);
        source = sourceRepository.saveAndFlush(source);

        Feed feed = new Feed();
        feed.setSource(source);
        feed.setFeedUrl("https://sauthz.test/feed-" + System.nanoTime());
        feed.setFeedType(Feed.FeedType.RSS);
        feed.setTitle("sauthz feed");
        feed = feedRepository.saveAndFlush(feed);

        Article created = new Article();
        created.setTitle("sauthz article");
        created.setOriginalUrl("https://sauthz.test/article-" + System.nanoTime());
        created.setContent("content");
        created.setPublicationDate(LocalDateTime.now());
        created.setFeed(feed);
        return articleRepository.saveAndFlush(created);
    }

    private Summary summary(User owner, String text) {
        Summary summary = new Summary();
        summary.setArticle(article);
        summary.setUser(owner);
        summary.setSummaryText(text);
        summary.setGeneratedAt(LocalDateTime.now());
        return summaryRepository.saveAndFlush(summary);
    }

    private SummaryJob job(User owner, SummaryJob.JobStatus status) {
        SummaryJob job = new SummaryJob();
        job.setArticle(article);
        job.setUser(owner);
        job.setStatus(status);
        job.setSubmittedAt(LocalDateTime.now());
        return summaryJobRepository.saveAndFlush(job);
    }

    private MockHttpServletRequestBuilder as(Account account, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token());
    }

    private String unique(String prefix) {
        return prefix + "-" + System.nanoTime();
    }

    private boolean hasActiveJobFor(Long userId) {
        return summaryJobRepository.existsByArticleIdAndUserIdAndStatusIn(article.getId(), userId,
                Arrays.asList(SummaryJob.JobStatus.QUEUED, SummaryJob.JobStatus.PROCESSING));
    }

    @Test
    @DisplayName("A summary by id: foreign is 404, own and shared are 200")
    void summaryByIdIsOwnerScoped() throws Exception {
        mockMvc.perform(as(alice, get("/api/v1/summaries/{id}", bobSummary.getId())))
                .andExpect(status().isNotFound());

        mockMvc.perform(as(alice, get("/api/v1/summaries/{id}", aliceSummary.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summaryText", is("Alice's personalized summary")));
        mockMvc.perform(as(alice, get("/api/v1/summaries/{id}", sharedSummary.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summaryText", is("The default summary")));

        mockMvc.perform(as(admin, get("/api/v1/summaries/{id}", bobSummary.getId())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("The article-wide listing returns only shared and own summaries")
    void articleListingExcludesOtherUsers() throws Exception {
        String body = mockMvc.perform(as(alice, get("/api/v1/summaries/article/{articleId}/all", article.getId())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Long> ids = objectMapper.readTree(body).findValues("id").stream()
                .map(JsonNode::asLong).toList();

        assertThat(ids).containsExactlyInAnyOrder(aliceSummary.getId(), sharedSummary.getId());
        assertThat(ids).doesNotContain(bobSummary.getId());
    }

    @Test
    @DisplayName("A job by id: foreign is 404, own and shared are 200")
    void jobByIdIsOwnerScoped() throws Exception {
        mockMvc.perform(as(alice, get("/api/v1/summaries/jobs/{jobId}", bobJob.getId())))
                .andExpect(status().isNotFound());

        mockMvc.perform(as(alice, get("/api/v1/summaries/jobs/{jobId}", aliceJob.getId())))
                .andExpect(status().isOk());
        mockMvc.perform(as(alice, get("/api/v1/summaries/jobs/{jobId}", sharedJob.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(as(admin, get("/api/v1/summaries/jobs/{jobId}", bobJob.getId())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Regenerating another user's summary is 404 and leaves it untouched")
    void regenerateIsOwnerScoped() throws Exception {
        mockMvc.perform(as(alice, post("/api/v1/summaries/{id}/regenerate", bobSummary.getId())))
                .andExpect(status().isNotFound());

        assertThat(summaryRepository.findById(bobSummary.getId()).orElseThrow().getRegenerationCount())
                .isZero();
        assertThat(hasActiveJobFor(bob.id())).as("no job may be created for Bob").isFalse();

        mockMvc.perform(as(alice, post("/api/v1/summaries/{id}/regenerate", aliceSummary.getId())))
                .andExpect(status().isAccepted());
        assertThat(hasActiveJobFor(alice.id())).isTrue();
    }

    @Test
    @DisplayName("Retrying another user's failed job is 404 and the job stays FAILED")
    void retryIsOwnerScoped() throws Exception {
        mockMvc.perform(as(alice, post("/api/v1/summaries/jobs/{jobId}/retry", bobFailedJob.getId())))
                .andExpect(status().isNotFound());

        SummaryJob afterDenied = summaryJobRepository.findById(bobFailedJob.getId()).orElseThrow();
        assertThat(afterDenied.getStatus()).isEqualTo(SummaryJob.JobStatus.FAILED);
        assertThat(afterDenied.getErrorMessage()).isEqualTo("worker failed");

        mockMvc.perform(as(bob, post("/api/v1/summaries/jobs/{jobId}/retry", bobFailedJob.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("QUEUED")));
        assertThat(summaryJobRepository.findById(bobFailedJob.getId()).orElseThrow().getStatus())
                .isEqualTo(SummaryJob.JobStatus.QUEUED);
    }

    @Test
    @DisplayName("Mutating a shared summary needs the worker or an admin")
    void sharedMutationNeedsWorkerOrAdmin() throws Exception {
        mockMvc.perform(as(alice, post("/api/v1/summaries/{id}/regenerate", sharedSummary.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is(FORBIDDEN_MESSAGE)));
        assertThat(summaryRepository.findById(sharedSummary.getId()).orElseThrow().getRegenerationCount())
                .isZero();

        mockMvc.perform(as(admin, post("/api/v1/summaries/{id}/regenerate", sharedSummary.getId())))
                .andExpect(status().isAccepted());
        assertThat(summaryRepository.findById(sharedSummary.getId()).orElseThrow().getRegenerationCount())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Retrying a shared failed job needs the worker or an admin")
    void sharedRetryNeedsWorkerOrAdmin() throws Exception {
        mockMvc.perform(as(alice, post("/api/v1/summaries/jobs/{jobId}/retry", sharedFailedJob.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is(FORBIDDEN_MESSAGE)));
        SummaryJob afterDenied = summaryJobRepository.findById(sharedFailedJob.getId()).orElseThrow();
        assertThat(afterDenied.getStatus()).isEqualTo(SummaryJob.JobStatus.FAILED);
        assertThat(afterDenied.getErrorMessage()).isEqualTo("worker failed");

        mockMvc.perform(as(admin, post("/api/v1/summaries/jobs/{jobId}/retry", sharedFailedJob.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("QUEUED")));
    }
}
