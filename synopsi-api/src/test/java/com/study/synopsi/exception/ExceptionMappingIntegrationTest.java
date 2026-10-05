package com.study.synopsi.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.synopsi.dto.LoginRequestDto;
import com.study.synopsi.dto.PasswordChangeDto;
import com.study.synopsi.dto.PasswordResetConfirmDto;
import com.study.synopsi.dto.UserRequestDto;
import com.study.synopsi.model.Article;
import com.study.synopsi.model.Feed;
import com.study.synopsi.model.Source;
import com.study.synopsi.model.User;
import com.study.synopsi.repository.ArticleRepository;
import com.study.synopsi.repository.FeedRepository;
import com.study.synopsi.repository.SourceRepository;
import com.study.synopsi.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives real controllers, services and the global handler over HTTP so the
 * status a client sees is the one asserted. Business failures must be 4xx:
 * the dashboard's fetchWithRetry retries any 5xx three times, and a 401 or 403
 * from a non-auth endpoint logs the user out, so a wrong current password has
 * to be a 400, not a 401 and not a 500.
 * Security filters are off: authorization is a separate defect.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("Business failures map to 4xx end to end")
class ExceptionMappingIntegrationTest {

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

    private static final String PASSWORD = "password123";

    private UserRequestDto registration(String suffix) {
        return UserRequestDto.builder()
                .username("mapping-" + suffix)
                .email("mapping-" + suffix + "@example.com")
                .password(PASSWORD)
                .build();
    }

    private Long register(UserRequestDto request) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("userId").asLong();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Test
    @DisplayName("Registering an email that already exists is a 409")
    void duplicateEmailOnRegisterIsConflict() throws Exception {
        String suffix = "dup-" + System.nanoTime();
        register(registration(suffix));

        UserRequestDto second = registration(suffix);
        second.setUsername("mapping-other-" + System.nanoTime());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Email already exists")));
    }

    @Test
    @DisplayName("A wrong current password is a 400 with a message, not a 401 and not a 500")
    void wrongCurrentPasswordIsBadRequest() throws Exception {
        Long userId = register(registration("pw-" + System.nanoTime()));

        mockMvc.perform(put("/api/v1/users/{id}/password", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PasswordChangeDto("not-the-password", "newpassword123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Current password is incorrect")));
    }

    @Test
    @DisplayName("An unknown user id is a 404")
    void unknownUserIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", 987654321L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("987654321")));
    }

    @Test
    @DisplayName("Login with an unknown account is a 401 that does not reveal which part was wrong")
    void unknownAccountOnLoginIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequestDto("nobody-" + System.nanoTime(), PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid username/email or password")));
    }

    @Test
    @DisplayName("Login to a disabled account is a 401")
    void disabledAccountOnLoginIsUnauthorized() throws Exception {
        UserRequestDto request = registration("disabled-" + System.nanoTime());
        Long userId = register(request);
        User user = userRepository.findById(userId).orElseThrow();
        user.setEnabled(false);
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequestDto(request.getUsername(), PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Account is disabled")));
    }

    @Test
    @DisplayName("Login to a locked account is a 401")
    void lockedAccountOnLoginIsUnauthorized() throws Exception {
        UserRequestDto request = registration("locked-" + System.nanoTime());
        Long userId = register(request);
        User user = userRepository.findById(userId).orElseThrow();
        user.setAccountLocked(true);
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequestDto(request.getUsername(), PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Account is locked")));
    }

    @Test
    @DisplayName("Confirming a password reset with an unknown token is a 400")
    void unknownResetTokenIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PasswordResetConfirmDto("no-such-token", "newpassword123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Invalid or expired reset token")));
    }

    @Test
    @DisplayName("Confirming a password reset with an expired token is a 400")
    void expiredResetTokenIsBadRequest() throws Exception {
        UserRequestDto request = registration("expired-" + System.nanoTime());
        Long userId = register(request);
        User user = userRepository.findById(userId).orElseThrow();
        String token = "expired-token-" + System.nanoTime();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().minusMinutes(1));
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PasswordResetConfirmDto(token, "newpassword123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Reset token has expired")));
    }

    @Test
    @DisplayName("A malformed JSON body is a 400 with a generic message, not a retried 500")
    void malformedBodyIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\": \"x\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Malformed request body")));
    }

    @Test
    @DisplayName("An unknown summary job is a 404")
    void unknownSummaryJobIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/summaries/jobs/{id}", 987654321L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("987654321")));
    }

    @Test
    @DisplayName("An unknown summary is a 404")
    void unknownSummaryIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/summaries/{id}", 987654321L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("987654321")));
    }

    @Test
    @DisplayName("Requesting a summary while one is already queued is a 409")
    void summaryAlreadyInProgressIsConflict() throws Exception {
        Source source = new Source();
        source.setName("mapping-source-" + System.nanoTime());
        source.setBaseUrl("https://mapping.test");
        source.setIsActive(true);
        source = sourceRepository.saveAndFlush(source);

        Feed feed = new Feed();
        feed.setSource(source);
        feed.setFeedUrl("https://mapping.test/feed-" + System.nanoTime());
        feed.setFeedType(Feed.FeedType.RSS);
        feed.setTitle("mapping feed");
        feed = feedRepository.saveAndFlush(feed);

        Article article = new Article();
        article.setTitle("mapping article");
        article.setOriginalUrl("https://mapping.test/article-" + System.nanoTime());
        article.setContent("content");
        article.setPublicationDate(LocalDateTime.now());
        article.setFeed(feed);
        article = articleRepository.saveAndFlush(article);

        mockMvc.perform(post("/api/v1/summaries/request")
                        .param("articleId", String.valueOf(article.getId())))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/summaries/request")
                        .param("articleId", String.valueOf(article.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", is("Summary generation already in progress")));
    }
}
