package com.marzouki.shop.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marzouki.shop.support.TestPasswords;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private ResultActions send(String path, Map<String, String> body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private JsonNode register(String email, String password) throws Exception {
        String body = send("/api/auth/register", Map.of("firstName", "Api", "lastName", "User",
                "email", email, "password", password))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    @Test
    void protectedEndpointsNeedAToken() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void anAccessTokenOpensTheProfile() throws Exception {
        JsonNode tokens = register("api@example.com", TestPasswords.strong());
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + tokens.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("api@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void wrongCredentialsGetAProblemResponse() throws Exception {
        send("/api/auth/login", Map.of("email", "ghost@example.com", "password", "nope"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void invalidRegistrationListsTheFieldErrors() throws Exception {
        send("/api/auth/register", Map.of("firstName", "A", "lastName", "B", "email", "bad", "password", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void refreshTokensRotateAndReuseRevokesEverything() throws Exception {
        String first = register("rotate@example.com", TestPasswords.strong()).get("refreshToken").asText();

        String body = send("/api/auth/refresh", Map.of("refreshToken", first))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String second = json.readTree(body).get("refreshToken").asText();
        assertThat(second).isNotEqualTo(first);

        send("/api/auth/refresh", Map.of("refreshToken", first)).andExpect(status().isUnauthorized());
        send("/api/auth/refresh", Map.of("refreshToken", second)).andExpect(status().isUnauthorized());
    }

    @Test
    void loggingOutRevokesTheRefreshToken() throws Exception {
        String password = TestPasswords.strong();
        register("bye@example.com", password);
        String body = send("/api/auth/login", Map.of("email", "bye@example.com", "password", password))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String refresh = json.readTree(body).get("refreshToken").asText();

        send("/api/auth/logout", Map.of("refreshToken", refresh)).andExpect(status().isNoContent());
        send("/api/auth/refresh", Map.of("refreshToken", refresh)).andExpect(status().isUnauthorized());
    }
}