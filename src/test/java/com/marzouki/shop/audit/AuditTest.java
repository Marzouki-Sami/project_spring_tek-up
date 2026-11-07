package com.marzouki.shop.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marzouki.shop.catalog.CategoryRepository;
import com.marzouki.shop.security.AppUserDetails;
import com.marzouki.shop.support.TestPasswords;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserRepository;
import com.marzouki.shop.user.UserService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuditTest {

    @Autowired MockMvc mvc;
    @Autowired UserService users;
    @Autowired UserRepository userRepository;
    @Autowired AuditRepository audit;
    @Autowired AuditService auditService;
    @Autowired CategoryRepository categories;

    User manager;
    User admin;

    @BeforeEach
    void data() {
        manager = users.create("Mo", "Manager", "mo@example.com", TestPasswords.strong(), Role.MANAGER);
        admin = users.create("Ada", "Admin", "ada@example.com", TestPasswords.strong(), Role.ADMIN);
    }

    @AfterEach
    void cleanUp() {
        audit.deleteAll();
        categories.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void changesByManagersAreRecordedWithWhoDidWhatAndTheResult() throws Exception {
        var asManager = user(new AppUserDetails(manager));
        mvc.perform(post("/manage/categories").with(asManager).with(csrf()).param("title", "Garden")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/manage/categories").with(asManager).with(csrf()).param("title", "")).andExpect(status().isOk());
        mvc.perform(get("/manage/categories").with(asManager)).andExpect(status().isOk());

        List<AuditEntry> entries = audit.findAll();
        assertThat(entries).hasSize(2);
        assertThat(entries).extracting(AuditEntry::getAction).containsOnly("categories.create");
        assertThat(entries).extracting(AuditEntry::getOutcome).containsExactlyInAnyOrder("OK", "REJECTED");
        assertThat(entries).allSatisfy(e -> {
            assertThat(e.getActorId()).isEqualTo(manager.getId());
            assertThat(e.getActorEmail()).isEqualTo("mo@example.com");
            assertThat(e.getMethod()).isEqualTo("POST");
            assertThat(e.getSummary()).contains("/manage/categories");
        });
    }

    @Test
    void customersAndSecretsNeverLeakIntoTheLog() throws Exception {
        var asAdmin = user(new AppUserDetails(admin));
        mvc.perform(post("/admin/users/" + manager.getId() + "/enabled").with(asAdmin).with(csrf()).param("enabled", "false"))
                .andExpect(status().is3xxRedirection());
        AuditEntry entry = audit.findAll().get(0);
        assertThat(entry.getAction()).isEqualTo("users.enabled");
        assertThat(entry.getEntityType()).isEqualTo("users");
        assertThat(entry.getEntityId()).isEqualTo(String.valueOf(manager.getId()));
        assertThat(entry.getSummary()).contains("enabled=false");
    }

    @Test
    void onlyAdminsMayReadTheLogAndItCanBeFiltered() throws Exception {
        auditService.record("products.update", "POST", "products", "5", AuditEntry.OK, "POST /manage/products/5");
        auditService.record("orders.status", "POST", "orders", "9", AuditEntry.REJECTED, "POST /manage/orders/9/status");

        mvc.perform(get("/admin/audit").with(user(new AppUserDetails(manager)))).andExpect(status().isForbidden());
        var asAdmin = user(new AppUserDetails(admin));
        mvc.perform(get("/admin/audit").with(asAdmin)).andExpect(status().isOk())
                .andExpect(content().string(containsString("products.update"))).andExpect(content().string(containsString("orders.status")));
        mvc.perform(get("/admin/audit").param("outcome", "REJECTED").with(asAdmin)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("products.update"))));
        mvc.perform(get("/admin/audit").param("q", "9").with(asAdmin)).andExpect(content().string(containsString("orders.status")));
        mvc.perform(get("/admin/audit").param("q", "%").with(asAdmin)).andExpect(content().string(containsString("Nothing matches")));
        mvc.perform(get("/admin/audit").param("from", "not-a-date").param("outcome", "bogus").with(asAdmin)).andExpect(status().isOk());
    }

    @Test
    void thereIsNoWayToChangeOrDeleteEntriesThroughTheApplication() throws Exception {
        auditService.record("x.y", "POST", "x", "1", AuditEntry.OK, "s");
        var asAdmin = user(new AppUserDetails(admin));
        mvc.perform(post("/admin/audit").with(asAdmin).with(csrf())).andExpect(status().is4xxClientError());
        mvc.perform(post("/admin/audit/1/delete").with(asAdmin).with(csrf())).andExpect(status().is4xxClientError());
        assertThat(audit.count()).isEqualTo(1);
    }
}