package com.marzouki.shop.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marzouki.shop.support.TestPasswords;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    private MockHttpServletRequestBuilder register(String email, String password) {
        return post("/register").with(csrf())
                .param("firstName", "Test").param("lastName", "User").param("email", email)
                .param("password", password).param("confirmPassword", password);
    }

    private MockHttpServletRequestBuilder login(String email, String password) {
        return post("/login").with(csrf()).param("username", email).param("password", password);
    }

    @Test
    void registrationCreatesAHashedCustomerAndSignsIn() throws Exception {
        String password = TestPasswords.strong();
        mvc.perform(register("new@example.com", password).param("role", "ADMIN"))
                .andExpect(redirectedUrl("/?welcome"))
                .andExpect(authenticated().withRoles("CUSTOMER"));

        User saved = users.findByEmailIgnoreCase("new@example.com").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(saved.getPasswordHash()).isNotEqualTo(password).startsWith("{bcrypt}");
    }

    @Test
    void weakPasswordsAreRefused() throws Exception {
        mvc.perform(register("weak@example.com", "short"))
                .andExpect(status().isOk())
                .andExpect(unauthenticated());
        assertThat(users.existsByEmailIgnoreCase("weak@example.com")).isFalse();
    }

    @Test
    void mismatchedPasswordsAreRefused() throws Exception {
        mvc.perform(register("mismatch@example.com", TestPasswords.strong())
                        .param("confirmPassword", TestPasswords.strong()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("do not match")));
    }

    @Test
    void anEmailCanOnlyRegisterOnce() throws Exception {
        String password = TestPasswords.strong();
        mvc.perform(register("dup@example.com", password));
        mvc.perform(register("DUP@example.com", password))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("already exists")));
    }

    @Test
    void signingInWorksAndAWrongPasswordIsRefused() throws Exception {
        String password = TestPasswords.strong();
        mvc.perform(register("login@example.com", password));

        mvc.perform(login("login@example.com", password))
                .andExpect(authenticated().withUsername("login@example.com"));
        mvc.perform(login("login@example.com", "nope"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void anAccountLocksAfterRepeatedFailures() throws Exception {
        String password = TestPasswords.strong();
        mvc.perform(register("lock@example.com", password));
        for (int i = 0; i < 5; i++) {
            mvc.perform(login("lock@example.com", "nope"));
        }
        mvc.perform(login("lock@example.com", password))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }
}