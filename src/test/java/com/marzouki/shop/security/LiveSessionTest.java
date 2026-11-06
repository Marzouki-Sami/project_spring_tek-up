package com.marzouki.shop.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marzouki.shop.support.TestPasswords;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserService;
import com.marzouki.shop.user.dto.PasswordChangeForm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** A website session must follow what happens to the account after sign-in. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LiveSessionTest {

    @Autowired MockMvc mvc;
    @Autowired UserService users;

    private MockHttpSession signIn(String email, String password) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("username", email).param("password", password))
                .andReturn().getRequest().getSession();
    }

    @Test
    void disablingAnAccountEndsItsSession() throws Exception {
        User admin = users.create("Ada", "Admin", "ada@example.com", TestPasswords.strong(), Role.ADMIN);
        String password = TestPasswords.strong();
        User victim = users.create("Vic", "Tim", "vic@example.com", password, Role.CUSTOMER);
        MockHttpSession session = signIn("vic@example.com", password);
        mvc.perform(get("/account").session(session)).andExpect(status().isOk());

        users.setEnabled(admin.getId(), victim.getId(), false);

        mvc.perform(get("/account").session(session)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void aRoleChangeAppliesToTheRunningSession() throws Exception {
        User admin = users.create("Ada", "Admin", "ada@example.com", TestPasswords.strong(), Role.ADMIN);
        String password = TestPasswords.strong();
        User person = users.create("Max", "Worker", "max@example.com", password, Role.MANAGER);
        MockHttpSession session = signIn("max@example.com", password);
        mvc.perform(get("/manage/products").session(session)).andExpect(status().isOk());

        users.changeRole(admin.getId(), person.getId(), Role.CUSTOMER);

        mvc.perform(get("/manage/products").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void changingThePasswordEndsOtherSessions() throws Exception {
        String password = TestPasswords.strong();
        User person = users.create("Pat", "Person", "pat@example.com", password, Role.CUSTOMER);
        MockHttpSession stolen = signIn("pat@example.com", password);
        mvc.perform(get("/account").session(stolen)).andExpect(status().isOk());

        PasswordChangeForm form = new PasswordChangeForm();
        form.setCurrentPassword(password);
        form.setNewPassword(TestPasswords.strong());
        form.setConfirmPassword(form.getNewPassword());
        users.changePassword(person.getId(), form);

        mvc.perform(get("/account").session(stolen)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void browsingAndTheApiDoNotCreateSessions() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(header().doesNotExist("Set-Cookie"));
        mvc.perform(get("/api/products")).andExpect(status().isOk()).andExpect(header().doesNotExist("Set-Cookie"));
    }
}