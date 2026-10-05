package com.study.synopsi.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.synopsi.dto.PasswordChangeDto;
import com.study.synopsi.dto.UserPreferenceDto;
import com.study.synopsi.dto.UserRequestDto;
import com.study.synopsi.model.User;
import com.study.synopsi.model.UserPreference;
import com.study.synopsi.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ownership over HTTP with the real security filter chain and real JWTs.
 * The user id in a path or query parameter must match the token's subject
 * unless the caller is an admin. Before this test existed any logged-in
 * user could read, change or delete any other user.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("A user can only act on their own resources")
class AuthorizationIntegrationTest {

    private static final String FORBIDDEN_MESSAGE = "You don't have permission to access this resource.";
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private record Account(Long id, String token) {}

    private Account register(String suffix) throws Exception {
        UserRequestDto request = UserRequestDto.builder()
                .username("authz-" + suffix)
                .email("authz-" + suffix + "@example.com")
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

    private MockHttpServletRequestBuilder as(Account account, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String unique(String prefix) {
        return prefix + "-" + System.nanoTime();
    }

    @Test
    @DisplayName("Reading another user's profile is 403, reading your own is 200")
    void profileIsOwnerOnly() throws Exception {
        Account alice = register(unique("alice"));
        Account bob = register(unique("bob"));

        mockMvc.perform(as(alice, get("/api/v1/users/{id}", bob.id())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", is(FORBIDDEN_MESSAGE)));

        mockMvc.perform(as(alice, get("/api/v1/users/{id}", alice.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(alice.id().intValue())));

        mockMvc.perform(as(alice, get("/api/v1/users/{id}/stats", bob.id())))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(alice, get("/api/v1/users/{id}/preferences", bob.id())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deleting another user is 403 and leaves the account enabled")
    void deleteIsOwnerOnly() throws Exception {
        Account alice = register(unique("alice"));
        Account bob = register(unique("bob"));

        mockMvc.perform(as(alice, delete("/api/v1/users/{id}", bob.id())))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(bob.id()).orElseThrow().getEnabled())
                .as("Bob must still be enabled").isTrue();
    }

    @Test
    @DisplayName("Changing another user's password or profile is 403")
    void passwordAndProfileChangesAreOwnerOnly() throws Exception {
        Account alice = register(unique("alice"));
        Account bob = register(unique("bob"));

        mockMvc.perform(as(alice, put("/api/v1/users/{id}/password", bob.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PasswordChangeDto(PASSWORD, "hijacked-password")))))
                .andExpect(status().isForbidden());

        mockMvc.perform(as(alice, put("/api/v1/users/{id}", bob.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Mallory\"}")))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(bob.id()).orElseThrow().getFirstName()).isNull();
    }

    @Test
    @DisplayName("Personalization endpoints are owner-only")
    void personalizationIsOwnerOnly() throws Exception {
        Account alice = register(unique("alice"));
        Account bob = register(unique("bob"));

        mockMvc.perform(as(alice, get("/api/v1/personalization/feed/{userId}", bob.id())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is(FORBIDDEN_MESSAGE)));
        mockMvc.perform(as(alice, get("/api/v1/personalization/preferences/{userId}", bob.id())))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(alice, get("/api/v1/personalization/interests/{userId}", bob.id())))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(alice, get("/api/v1/personalization/similar/{userId}/{articleId}", bob.id(), 1L)))
                .andExpect(status().isForbidden());

        UserPreferenceDto preference = UserPreferenceDto.builder()
                .topicId(1L)
                .interestLevel(UserPreference.InterestLevel.HIGH)
                .build();
        mockMvc.perform(as(alice, put("/api/v1/personalization/preferences/{userId}", bob.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(preference))))
                .andExpect(status().isForbidden());

        mockMvc.perform(as(alice, post("/api/v1/personalization/interactions/{userId}/read", bob.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleId\":1,\"timeSpentSeconds\":10}")))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(alice, post("/api/v1/personalization/interactions/{userId}/feedback", bob.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleId\":1,\"feedbackType\":\"LIKED\"}")))
                .andExpect(status().isForbidden());

        // Own resources still work
        mockMvc.perform(as(alice, get("/api/v1/personalization/preferences/{userId}", alice.id())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("User-scoped summary endpoints are owner-only")
    void summariesAreOwnerOnly() throws Exception {
        Account alice = register(unique("alice"));
        Account bob = register(unique("bob"));

        mockMvc.perform(as(alice, get("/api/v1/summaries/user/{userId}", bob.id())))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(alice, get("/api/v1/summaries/article/{articleId}", 1L)
                        .param("userId", String.valueOf(bob.id()))))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(alice, post("/api/v1/summaries/request")
                        .param("articleId", "1")
                        .param("userId", String.valueOf(bob.id()))))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(alice, get("/api/v1/summaries/exists")
                        .param("articleId", "1")
                        .param("summaryType", "BRIEF")
                        .param("userId", String.valueOf(bob.id()))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User administration is admin-only")
    void administrationIsAdminOnly() throws Exception {
        Account alice = register(unique("alice"));
        Account bob = register(unique("bob"));

        mockMvc.perform(as(alice, get("/api/v1/users")))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(alice, get("/api/v1/users/search").param("term", "bob")))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(alice, get("/api/v1/users/enabled")))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(alice, get("/api/v1/users/role/{role}", "USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(alice, get("/api/v1/users/email/{email}", "x@example.com")))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(alice, put("/api/v1/users/{id}/disable", bob.id())))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(alice, put("/api/v1/users/{id}/lock", bob.id())))
                .andExpect(status().isForbidden());

        User bobRow = userRepository.findById(bob.id()).orElseThrow();
        assertThat(bobRow.getEnabled()).isTrue();
        assertThat(bobRow.getAccountLocked()).isFalse();
    }

    @Test
    @DisplayName("An admin can act on any user")
    void adminCanActOnAnyUser() throws Exception {
        Account admin = registerAdmin(unique("admin"));
        Account bob = register(unique("bob"));

        mockMvc.perform(as(admin, get("/api/v1/users/{id}", bob.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(bob.id().intValue())));
        mockMvc.perform(as(admin, get("/api/v1/users")))
                .andExpect(status().isOk());
        mockMvc.perform(as(admin, get("/api/v1/personalization/preferences/{userId}", bob.id())))
                .andExpect(status().isOk());
        mockMvc.perform(as(admin, put("/api/v1/users/{id}/disable", bob.id())))
                .andExpect(status().isNoContent());

        // setEnabled is a bulk JPQL update that bypasses the persistence context
        entityManager.clear();
        assertThat(userRepository.findById(bob.id()).orElseThrow().getEnabled()).isFalse();
    }

    @Test
    @DisplayName("No token is 401, not 403")
    void missingTokenIsUnauthorized() throws Exception {
        Account alice = register(unique("alice"));

        mockMvc.perform(get("/api/v1/users/{id}", alice.id()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A disabled account's unexpired token is rejected with 401")
    void disabledAccountTokenIsRejected() throws Exception {
        Account alice = register(unique("alice"));
        mockMvc.perform(as(alice, get("/api/v1/users/{id}", alice.id())))
                .andExpect(status().isOk());

        User row = userRepository.findById(alice.id()).orElseThrow();
        row.setEnabled(false);
        userRepository.saveAndFlush(row);

        mockMvc.perform(as(alice, get("/api/v1/users/{id}", alice.id())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Account is disabled.")));
    }

    @Test
    @DisplayName("A locked account's unexpired token is rejected with 401")
    void lockedAccountTokenIsRejected() throws Exception {
        Account alice = register(unique("alice"));

        User row = userRepository.findById(alice.id()).orElseThrow();
        row.setAccountLocked(true);
        userRepository.saveAndFlush(row);

        mockMvc.perform(as(alice, get("/api/v1/users/{id}", alice.id())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Account is locked.")));
    }

    @Test
    @DisplayName("Deleting your own account invalidates the token you did it with")
    void deletedAccountTokenIsRejected() throws Exception {
        Account alice = register(unique("alice"));

        mockMvc.perform(as(alice, delete("/api/v1/users/{id}", alice.id())))
                .andExpect(status().isNoContent());

        mockMvc.perform(as(alice, get("/api/v1/users/{id}", alice.id())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A disabled admin cannot use its old token to re-enable itself")
    void disabledAdminCannotRestoreItself() throws Exception {
        Account admin = registerAdmin(unique("admin"));

        User row = userRepository.findById(admin.id()).orElseThrow();
        row.setEnabled(false);
        userRepository.saveAndFlush(row);

        mockMvc.perform(as(admin, put("/api/v1/users/{id}/enable", admin.id())))
                .andExpect(status().isUnauthorized());

        entityManager.clear();
        assertThat(userRepository.findById(admin.id()).orElseThrow().getEnabled())
                .as("the admin must still be disabled").isFalse();
    }
}
