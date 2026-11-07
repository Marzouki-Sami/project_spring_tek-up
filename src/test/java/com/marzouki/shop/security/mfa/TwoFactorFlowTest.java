package com.marzouki.shop.security.mfa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marzouki.shop.audit.AuditRepository;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.security.AppUserDetails;
import com.marzouki.shop.support.TestPasswords;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserRepository;
import com.marzouki.shop.user.UserService;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.mfa.staff2fa=true")
class TwoFactorFlowTest {

    @Autowired MockMvc mvc;
    @Autowired UserService users;
    @Autowired UserRepository userRepository;
    @Autowired TwoFactorService twoFactor;
    @Autowired AuditRepository audit;

    String password;
    User manager;

    @BeforeEach
    void data() {
        audit.deleteAll();
        password = TestPasswords.strong();
        manager = users.create("Mo", "Manager", "mo@example.com", password, Role.MANAGER);
    }

    @AfterEach
    void cleanUp() {
        audit.deleteAll();
        userRepository.deleteAll();
    }

    private String currentCode(String secret) {
        return Totp.code(secret, Totp.currentStep(System.currentTimeMillis()));
    }

    /** Enrols the manager through the website, returning the secret and the recovery codes. */
    private record Enrolment(String secret, List<String> recoveryCodes) {
    }

    @SuppressWarnings("unchecked")
    private Enrolment enrol(User who) throws Exception {
        MockHttpSession session = new MockHttpSession();
        var me = user(new AppUserDetails(who));
        mvc.perform(get("/2fa/setup").session(session).with(me)).andExpect(status().isOk())
                .andExpect(content().string(containsString("data:image/png;base64,")));
        String secret = (String) session.getAttribute("mfa.pendingSecret");
        MvcResult done = mvc.perform(post("/2fa/setup").session(session).with(me).with(csrf()).param("code", currentCode(secret)))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/2fa/recovery-codes")).andReturn();
        List<String> codes = (List<String>) done.getFlashMap().get("recoveryCodes");
        assertThat(session.getAttribute(MfaLoginSuccessHandler.VERIFIED)).isEqualTo(Boolean.TRUE);
        return new Enrolment(secret, codes);
    }

    @Test
    void aManagerWithoutTwoFactorIsSentToSetItUpAndNothingElse() throws Exception {
        var me = user(new AppUserDetails(manager));
        mvc.perform(get("/manage/products").with(me)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/2fa/setup"));
        mvc.perform(get("/account").with(me)).andExpect(redirectedUrl("/2fa/setup"));
        mvc.perform(get("/products").with(me)).andExpect(redirectedUrl("/2fa/setup"));
        mvc.perform(get("/2fa/setup").with(me)).andExpect(status().isOk());
    }

    @Test
    void customersAreNotForcedToUseTwoFactor() throws Exception {
        User customer = users.create("Bea", "Buyer", "bea@example.com", TestPasswords.strong(), Role.CUSTOMER);
        mvc.perform(get("/account").with(user(new AppUserDetails(customer)))).andExpect(status().isOk());
    }

    @Test
    void settingUpTurnsItOnAndUnlocksTheSession() throws Exception {
        Enrolment enrolment = enrol(manager);
        assertThat(enrolment.recoveryCodes()).hasSize(10).allMatch(c -> c.matches("[A-Z2-9]{5}-[A-Z2-9]{5}"));
        User saved = userRepository.findById(manager.getId()).orElseThrow();
        assertThat(saved.isTotpEnabled()).isTrue();
        assertThat(saved.getTotpSecret()).isNotEqualTo(enrolment.secret()).doesNotContain(enrolment.secret());
        assertThat(audit.findAll()).anyMatch(e -> e.getAction().equals("2fa.enabled"));
    }

    @Test
    void aWrongCodeDuringSetupChangesNothing() throws Exception {
        MockHttpSession session = new MockHttpSession();
        var me = user(new AppUserDetails(manager));
        mvc.perform(get("/2fa/setup").session(session).with(me)).andExpect(status().isOk());
        mvc.perform(post("/2fa/setup").session(session).with(me).with(csrf()).param("code", "000000"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("That code is not right")));
        assertThat(userRepository.findById(manager.getId()).orElseThrow().isTotpEnabled()).isFalse();
    }

    @Test
    void anEnrolledUserMustVerifyEachNewSession() throws Exception {
        Enrolment enrolment = enrol(manager);
        User fresh = userRepository.findById(manager.getId()).orElseThrow();
        var me = user(new AppUserDetails(fresh));

        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/manage/products").session(session).with(me)).andExpect(redirectedUrl("/2fa/verify"));
        mvc.perform(post("/2fa/verify").session(session).with(me).with(csrf()).param("code", "123456"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("not right")));
        mvc.perform(get("/manage/products").session(session).with(me)).andExpect(redirectedUrl("/2fa/verify"));

        long step = Totp.currentStep(System.currentTimeMillis());
        String code = Totp.code(enrolment.secret(), step + 1);
        mvc.perform(post("/2fa/verify").session(session).with(me).with(csrf()).param("code", code))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        mvc.perform(get("/manage/products").session(session).with(me)).andExpect(status().isOk());

        MockHttpSession other = new MockHttpSession();
        mvc.perform(post("/2fa/verify").session(other).with(me).with(csrf()).param("code", code))
                .andExpect(status().isOk()).andExpect(content().string(containsString("not right")));
    }

    @Test
    void aRecoveryCodeWorksOnce() throws Exception {
        Enrolment enrolment = enrol(manager);
        String recovery = enrolment.recoveryCodes().get(0);
        assertThat(twoFactor.verify(manager.getId(), recovery.toLowerCase())).isTrue();
        assertThat(twoFactor.verify(manager.getId(), recovery)).isFalse();
        assertThat(twoFactor.recoveryCodesLeft(manager.getId())).isEqualTo(9);
    }

    @Test
    void fiveWrongCodesBlockFurtherTries() throws Exception {
        Enrolment enrolment = enrol(manager);
        for (int i = 0; i < 5; i++) {
            assertThat(twoFactor.verify(manager.getId(), "000000")).isFalse();
        }
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> twoFactor.verify(manager.getId(), currentCode(enrolment.secret())))
                .isInstanceOf(ConflictException.class).hasMessageContaining("Too many");
    }

    @Test
    void aTrustedDeviceSkipsTheCodeAtPasswordSignInUntilThePasswordChanges() throws Exception {
        Enrolment enrolment = enrol(manager);
        var me = user(new AppUserDetails(userRepository.findById(manager.getId()).orElseThrow()));
        MockHttpSession session = new MockHttpSession();
        MvcResult verified = mvc.perform(post("/2fa/verify").session(session).with(me).with(csrf())
                        .param("code", Totp.code(enrolment.secret(), Totp.currentStep(System.currentTimeMillis()) + 1)).param("trust", "true"))
                .andExpect(header().string("Set-Cookie", containsString("shop_device="))).andReturn();
        String header = verified.getResponse().getHeader("Set-Cookie");
        assertThat(header).contains("HttpOnly").contains("SameSite=Lax");
        String token = header.substring(header.indexOf('=') + 1, header.indexOf(';'));

        MvcResult withCookie = mvc.perform(post("/login").with(csrf()).param("username", "mo@example.com").param("password", password)
                .cookie(new Cookie("shop_device", token))).andReturn();
        assertThat(withCookie.getRequest().getSession().getAttribute(MfaLoginSuccessHandler.VERIFIED)).isEqualTo(Boolean.TRUE);

        MvcResult withoutCookie = mvc.perform(post("/login").with(csrf()).param("username", "mo@example.com").param("password", password)).andReturn();
        assertThat(withoutCookie.getRequest().getSession().getAttribute(MfaLoginSuccessHandler.VERIFIED)).isNull();

        MvcResult forged = mvc.perform(post("/login").with(csrf()).param("username", "mo@example.com").param("password", password)
                .cookie(new Cookie("shop_device", "not-a-real-token"))).andReturn();
        assertThat(forged.getRequest().getSession().getAttribute(MfaLoginSuccessHandler.VERIFIED)).isNull();

        twoFactor.forgetDevices(manager.getId());
        MvcResult forgotten = mvc.perform(post("/login").with(csrf()).param("username", "mo@example.com").param("password", password)
                .cookie(new Cookie("shop_device", token))).andReturn();
        assertThat(forgotten.getRequest().getSession().getAttribute(MfaLoginSuccessHandler.VERIFIED)).isNull();
    }

    @Test
    void staffCannotTurnItOffButAdminsCanResetIt() throws Exception {
        enrol(manager);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> twoFactor.disable(manager.getId()))
                .isInstanceOf(ConflictException.class);
        twoFactor.reset(manager.getId());
        User cleared = userRepository.findById(manager.getId()).orElseThrow();
        assertThat(cleared.isTotpEnabled()).isFalse();
        assertThat(cleared.getTotpSecret()).isNull();
        assertThat(twoFactor.recoveryCodesLeft(manager.getId())).isZero();
        assertThat(audit.findAll()).anyMatch(e -> e.getAction().equals("2fa.reset"));
    }

    @Test
    void customersMayTurnItOnAndOffThemselves() throws Exception {
        User customer = users.create("Bea", "Buyer", "bea@example.com", TestPasswords.strong(), Role.CUSTOMER);
        enrol(customer);
        MockHttpSession session = new MockHttpSession();
        var me = user(new AppUserDetails(userRepository.findById(customer.getId()).orElseThrow()));
        mvc.perform(get("/account").session(session).with(me)).andExpect(redirectedUrl("/2fa/verify"));
        twoFactor.disable(customer.getId());
        mvc.perform(get("/account").with(user(new AppUserDetails(userRepository.findById(customer.getId()).orElseThrow()))))
                .andExpect(status().isOk());
    }

    @Test
    void theApiAsksStaffForTheCodeToo() throws Exception {
        String body = "{\"email\":\"mo@example.com\",\"password\":\"%s\"%s}";
        mvc.perform(post("/api/auth/login").contentType("application/json").content(body.formatted(password, "")))
                .andExpect(status().isForbidden());
        Enrolment enrolment = enrol(manager);
        mvc.perform(post("/api/auth/login").contentType("application/json").content(body.formatted(password, "")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(body.formatted(password, ",\"otp\":\"" + enrolment.recoveryCodes().get(0) + "\"")))
                .andExpect(status().isOk());
    }

    @Test
    void anAdminCanResetSomeonesTwoFactorFromTheUserList() throws Exception {
        enrol(manager);
        User admin = users.create("Ada", "Admin", "ada@example.com", TestPasswords.strong(), Role.ADMIN);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(MfaLoginSuccessHandler.VERIFIED, Boolean.TRUE);
        mvc.perform(post("/admin/users/" + manager.getId() + "/reset-2fa").session(session)
                        .with(user(new AppUserDetails(admin))).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(userRepository.findById(manager.getId()).orElseThrow().isTotpEnabled()).isFalse();
    }
}