package com.study.synopsi.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.synopsi.dto.LoginRequestDto;
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
import com.study.synopsi.service.WorkerAccountSeedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The summarization worker's routes (the queued-job list and the two
 * callbacks) act on every user's jobs, so they must be reserved for the
 * worker identity the API seeds from configuration. Before this test
 * existed any registered user could list all queued jobs and complete or
 * fail another user's job with text of their choosing.
 *
 * The worker credentials here are distinct from the ones
 * WorkerAccountSeedServiceIntegrationTest uses so the two contexts do not
 * share a seeded row.
 */
@SpringBootTest(properties = {
        "synopsi.worker.username=authz-worker",
        "synopsi.worker.password=authz-worker-pass"
})
@AutoConfigureMockMvc
@Transactional
@DisplayName("Worker routes are reserved for the worker identity")
class WorkerAuthorizationIntegrationTest {

    private static final String PASSWORD = "password123";

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
    private SummaryJobRepository summaryJobRepository;

    @Autowired
    private SummaryRepository summaryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private record Account(Long id, String token) {}

    private Account alice;
    private Account bob;
    private Article article;
    private SummaryJob bobJob;

    @BeforeEach
    void setUp() throws Exception {
        alice = register(unique("alice"));
        bob = register(unique("bob"));
        article = createArticle();

        SummaryJob job = new SummaryJob();
        job.setArticle(article);
        job.setUser(userRepository.findById(bob.id()).orElseThrow());
        job.setSummaryType(Summary.SummaryType.BRIEF);
        job.setSummaryLength(Summary.SummaryLength.MEDIUM);
        job.setStatus(SummaryJob.JobStatus.QUEUED);
        job.setSubmittedAt(LocalDateTime.now());
        bobJob = summaryJobRepository.saveAndFlush(job);
    }

    private Account register(String suffix) throws Exception {
        UserRequestDto request = UserRequestDto.builder()
                .username("wauthz-" + suffix)
                .email("wauthz-" + suffix + "@example.com")
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

    private String loginAsWorker() throws Exception {
        return login("authz-worker", "authz-worker-pass");
    }

    private String login(String username, String password) throws Exception {
        LoginRequestDto login = new LoginRequestDto(username, password);
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private Article createArticle() {
        Source source = new Source();
        source.setName("wauthz-source-" + System.nanoTime());
        source.setBaseUrl("https://wauthz.test");
        source.setIsActive(true);
        source = sourceRepository.saveAndFlush(source);

        Feed feed = new Feed();
        feed.setSource(source);
        feed.setFeedUrl("https://wauthz.test/feed-" + System.nanoTime());
        feed.setFeedType(Feed.FeedType.RSS);
        feed.setTitle("wauthz feed");
        feed = feedRepository.saveAndFlush(feed);

        Article created = new Article();
        created.setTitle("wauthz article");
        created.setOriginalUrl("https://wauthz.test/article-" + System.nanoTime());
        created.setContent("content");
        created.setPublicationDate(LocalDateTime.now());
        created.setFeed(feed);
        return articleRepository.saveAndFlush(created);
    }

    private MockHttpServletRequestBuilder withToken(String token, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String unique(String prefix) {
        return prefix + "-" + System.nanoTime();
    }

    private SummaryJob reload(SummaryJob job) {
        return summaryJobRepository.findById(job.getId()).orElseThrow();
    }

    @Test
    @DisplayName("An ordinary user cannot list the queue or read job statistics")
    void ordinaryUserCannotListQueuedJobs() throws Exception {
        mockMvc.perform(withToken(alice.token(), get("/api/v1/summaries/jobs/queued")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is(AccessControlServiceMessages.FORBIDDEN)));
        mockMvc.perform(withToken(alice.token(), get("/api/v1/summaries/jobs/statistics")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An ordinary user cannot complete another user's job")
    void ordinaryUserCannotCompleteAnotherUsersJob() throws Exception {
        mockMvc.perform(withToken(alice.token(), post("/api/v1/summaries/callback/complete")
                        .param("jobId", String.valueOf(bobJob.getId()))
                        .param("summaryText", "Alice wrote this")
                        .param("modelVersion", "alice-v1")))
                .andExpect(status().isForbidden());

        SummaryJob after = reload(bobJob);
        assertThat(after.getStatus()).isEqualTo(SummaryJob.JobStatus.QUEUED);
        assertThat(after.getCompletedAt()).isNull();
        assertThat(summaryRepository.findByArticleId(article.getId()))
                .as("no summary row may be created by a denied callback").isEmpty();
    }

    @Test
    @DisplayName("An ordinary user cannot fail another user's job")
    void ordinaryUserCannotFailAnotherUsersJob() throws Exception {
        mockMvc.perform(withToken(alice.token(), post("/api/v1/summaries/callback/failure")
                        .param("jobId", String.valueOf(bobJob.getId()))
                        .param("errorMessage", "sabotage")))
                .andExpect(status().isForbidden());

        SummaryJob after = reload(bobJob);
        assertThat(after.getStatus()).isEqualTo(SummaryJob.JobStatus.QUEUED);
        assertThat(after.getAttempts()).isZero();
        assertThat(after.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("The seeded worker logs in, sees the queue and completes the job")
    void seededWorkerCanPollAndComplete() throws Exception {
        User seeded = userRepository.findByUsername("authz-worker").orElseThrow();
        assertThat(seeded.getRole()).isEqualTo(User.UserRole.WORKER);

        String workerToken = loginAsWorker();

        String body = mockMvc.perform(withToken(workerToken, get("/api/v1/summaries/jobs/queued")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Long> ids = objectMapper.readValue(body, JsonNode.class).findValuesAsText("id")
                .stream().map(Long::valueOf).toList();
        assertThat(ids).contains(bobJob.getId());

        mockMvc.perform(withToken(workerToken, post("/api/v1/summaries/callback/complete")
                        .param("jobId", String.valueOf(bobJob.getId()))
                        .param("summaryText", "Generated by the worker")
                        .param("modelVersion", "test-model")))
                .andExpect(status().isOk());

        SummaryJob after = reload(bobJob);
        assertThat(after.getStatus()).isEqualTo(SummaryJob.JobStatus.COMPLETED);
        List<Summary> summaries = summaryRepository.findByArticleId(article.getId());
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getSummaryText()).isEqualTo("Generated by the worker");
        assertThat(summaries.get(0).getUser().getId()).isEqualTo(bob.id());

        mockMvc.perform(withToken(workerToken, get("/api/v1/summaries/jobs/statistics")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Seeding does not promote an unrelated account that merely shares the configured username")
    void seedingRefusesUsernameCollision() throws Exception {
        // An ordinary person registered this name, with their own password,
        // before the deployment chose it for the worker.
        String name = "wauthz-" + unique("collide");
        Account owner = register(name.substring("wauthz-".length()));
        String hashBefore = userRepository.findById(owner.id()).orElseThrow().getPassword();

        WorkerAccountSeedService seeder = new WorkerAccountSeedService(
                userRepository, passwordEncoder, name, "a-different-worker-pass", "");
        assertThatThrownBy(seeder::seedWorkerAccount)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not match");

        User after = userRepository.findById(owner.id()).orElseThrow();
        assertThat(after.getRole()).isEqualTo(User.UserRole.USER);
        assertThat(after.getPassword()).isEqualTo(hashBefore);

        mockMvc.perform(withToken(owner.token(), get("/api/v1/summaries/jobs/queued")))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(login(name, PASSWORD), get("/api/v1/summaries/jobs/queued")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Seeding promotes a legacy USER worker account whose configured password verifies")
    void seedingMigratesLegacyWorkerAccount() throws Exception {
        // An earlier version seeded the worker as USER with this password.
        String name = "wauthz-" + unique("legacy");
        Account legacy = register(name.substring("wauthz-".length()));
        String hashBefore = userRepository.findById(legacy.id()).orElseThrow().getPassword();

        WorkerAccountSeedService seeder = new WorkerAccountSeedService(
                userRepository, passwordEncoder, name, PASSWORD, "");
        assertThat(seeder.seedWorkerAccount()).isFalse();

        User after = userRepository.findById(legacy.id()).orElseThrow();
        assertThat(after.getRole()).isEqualTo(User.UserRole.WORKER);
        assertThat(after.getPassword()).isEqualTo(hashBefore);

        mockMvc.perform(withToken(login(name, PASSWORD), get("/api/v1/summaries/jobs/queued")))
                .andExpect(status().isOk());
    }

    /** Keeps the asserted message in one place without reaching into the service. */
    private static final class AccessControlServiceMessages {
        static final String FORBIDDEN = "You don't have permission to access this resource.";
    }
}
