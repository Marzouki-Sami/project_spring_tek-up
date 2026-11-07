package com.marzouki.shop.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.security.AppUserDetails;
import com.marzouki.shop.settings.SettingsForm;
import com.marzouki.shop.settings.SettingsService;
import com.marzouki.shop.support.TestPasswords;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserRepository;
import com.marzouki.shop.user.UserService;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(EmailTemplatesTest.Recording.class)
class EmailTemplatesTest {

    static final List<Email> SENT = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class Recording {
        @Bean
        @Primary
        MailOutbox recordingOutbox() {
            return SENT::add;
        }
    }

    @Autowired MockMvc mvc;
    @Autowired EmailTemplates templates;
    @Autowired MailComposer composer;
    @Autowired SettingsService settings;
    @Autowired UserService users;
    @Autowired UserRepository userRepository;
    @Autowired EmailTemplateRepository templateRepository;

    User admin;
    User manager;

    @BeforeEach
    void data() {
        SENT.clear();
        admin = users.create("Ada", "Admin", "ada@example.com", TestPasswords.strong(), Role.ADMIN);
        manager = users.create("Mo", "Manager", "mo@example.com", TestPasswords.strong(), Role.MANAGER);
    }

    @AfterEach
    void cleanUp() {
        templateRepository.deleteAll();
        SettingsForm original = settings.form();
        original.setShopName("Shop");
        original.setAccentColor("#4f46e5");
        original.setSupportEmail(null);
        original.setVatPercent(new BigDecimal("20.0"));
        original.setVatNumber(null);
        original.setFooterText("You received this email because of activity on your Shop account.");
        settings.save(original);
        userRepository.deleteAll();
    }

    private EmailTemplateForm form(String subject, String heading, String body, String button) {
        EmailTemplateForm form = new EmailTemplateForm();
        form.setSubject(subject);
        form.setHeading(heading);
        form.setBody(body);
        form.setButtonLabel(button);
        return form;
    }

    @Test
    void unchangedEmailsUseTheirBuiltInWording() {
        EmailTemplates.Wording wording = templates.resolve(EmailKind.RESET);
        assertThat(wording.custom()).isFalse();
        Email email = composer.passwordReset(admin, "tok123");
        assertThat(email.subject()).isEqualTo("Reset your password");
        assertThat(email.html()).contains("Choose a new password").contains("/reset-password?token=tok123");
        assertThat(email.text()).contains("Hi Ada").contains("/reset-password?token=tok123");
    }

    @Test
    void editedWordingIsUsedWithPlaceholdersFilledIn() {
        templates.save(EmailKind.RESET, form("{{shop}}: new password for {{name}}", "Hello {{ name }}",
                "Use the button.\n\nSee you soon at {{shop}}.", "Go"), admin.getId());
        Email email = composer.passwordReset(admin, "tok123");
        assertThat(email.subject()).isEqualTo("Shop: new password for Ada");
        assertThat(email.html()).contains("Hello Ada").contains("Use the button.").contains("See you soon at Shop.").contains(">Go<");
        assertThat(email.text()).contains("Hello Ada").contains("Go: ");
        assertThat(templates.isCustom(EmailKind.RESET)).isTrue();

        templates.reset(EmailKind.RESET);
        assertThat(composer.passwordReset(admin, "tok123").subject()).isEqualTo("Reset your password");
    }

    @Test
    void markupInTheWordingIsShownAsTextNeverExecuted() {
        templates.save(EmailKind.RESET, form("Hi", "<script>alert(1)</script>", "<b>bold</b> [[${7*7}]] ${7*7} __${T(java.lang.Runtime)}__", ""),
                admin.getId());
        Email email = composer.passwordReset(admin, "tok123");
        assertThat(email.html()).doesNotContain("<script>").contains("&lt;script&gt;").doesNotContain("<b>bold</b>");
        assertThat(email.html()).contains("[[${7*7}]]".replace("[", "[").replace("$", "$"));
        assertThat(email.html()).doesNotContain(">49<");
    }

    @Test
    void headerInjectionInTheSubjectIsRemoved() {
        templates.save(EmailKind.RESET, form("Reset\nBcc: evil@example.com", "Hi", "Body", ""), admin.getId());
        assertThat(composer.passwordReset(admin, "tok").subject()).doesNotContain("\n").doesNotContain("\r");
    }

    @Test
    void unknownOrBrokenPlaceholdersAreRefused() {
        assertThatThrownBy(() -> templates.save(EmailKind.RESET, form("Hi", "Hi", "Order {{orderNumber}}", ""), admin.getId()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("{{orderNumber}}");
        assertThatThrownBy(() -> templates.save(EmailKind.RESET, form("Hi {{name", "Hi", "Body", ""), admin.getId()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("not written correctly");
        assertThat(templates.isCustom(EmailKind.RESET)).isFalse();
    }

    @Test
    void brandingAppliesToEveryEmail() {
        SettingsForm form = settings.form();
        form.setShopName("Lumen Goods");
        form.setAccentColor("#ff0066");
        form.setSupportEmail("help@lumen.test");
        settings.save(form);
        Email email = composer.passwordReset(admin, "tok");
        assertThat(email.html()).contains("Lumen Goods").contains("#ff0066").contains("help@lumen.test");
        assertThat(email.text()).contains("Lumen Goods");
        assertThat(settings.shopName()).isEqualTo("Lumen Goods");
    }

    @Test
    void everyKindRendersWithSampleData() {
        for (EmailKind kind : EmailKind.values()) {
            Email email = composer.sample(kind, "ada@example.com");
            assertThat(email.subject()).as(kind.name()).isNotBlank().doesNotContain("{{");
            assertThat(email.html()).as(kind.name()).doesNotContain("{{");
            assertThat(composer.preview(kind, templates.resolve(kind)).paragraphs()).isNotEmpty();
        }
    }

    @Test
    void adminsEditPreviewTestAndResetThroughThePages() throws Exception {
        var asAdmin = user(new AppUserDetails(admin));
        mvc.perform(get("/admin/emails").with(asAdmin)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Order confirmation")));
        mvc.perform(get("/admin/emails/reset").with(asAdmin)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Reset your password")));
        mvc.perform(get("/admin/emails/nope").with(asAdmin)).andExpect(status().isNotFound());

        mvc.perform(post("/admin/emails/reset/preview").with(asAdmin).with(csrf()).param("subject", "S").param("heading", "Fresh heading")
                        .param("body", "Hello {{name}}").param("buttonLabel", "Click"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Fresh heading"))).andExpect(content().string(containsString("Hello Alex")));
        assertThat(templates.isCustom(EmailKind.RESET)).isFalse();

        mvc.perform(post("/admin/emails/reset").with(asAdmin).with(csrf()).param("subject", "S").param("heading", "H")
                        .param("body", "Hello {{name}}").param("buttonLabel", "Click"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("success"));
        assertThat(templates.isCustom(EmailKind.RESET)).isTrue();

        mvc.perform(post("/admin/emails/reset").with(asAdmin).with(csrf()).param("subject", "S").param("heading", "H").param("body", "{{bogus}}"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("not available in this email")));
        mvc.perform(post("/admin/emails/reset").with(asAdmin).with(csrf()).param("subject", "").param("heading", "H").param("body", "B"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Enter a subject")));

        mvc.perform(post("/admin/emails/reset/test").with(asAdmin).with(csrf())).andExpect(flash().attributeExists("info"));
        assertThat(SENT).singleElement().satisfies(mail -> {
            assertThat(mail.to()).isEqualTo("ada@example.com");
            assertThat(mail.html()).contains("Hello Alex");
        });

        mvc.perform(post("/admin/emails/reset/reset").with(asAdmin).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(templates.isCustom(EmailKind.RESET)).isFalse();
    }

    @Test
    void settingsPageValidatesAndSaves() throws Exception {
        var asAdmin = user(new AppUserDetails(admin));
        mvc.perform(get("/admin/settings").with(asAdmin)).andExpect(status().isOk());
        mvc.perform(post("/admin/settings").with(asAdmin).with(csrf()).param("shopName", "Lumen").param("accentColor", "red")
                        .param("footerText", "f").param("vatPercent", "20"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Use a colour like")));
        mvc.perform(post("/admin/settings").with(asAdmin).with(csrf()).param("shopName", "Lumen").param("accentColor", "#00aa88")
                        .param("footerText", "Thanks").param("vatPercent", "19.6").param("vatNumber", "FR123"))
                .andExpect(status().is3xxRedirection());
        assertThat(settings.get().getVatPercent()).isEqualByComparingTo(new BigDecimal("19.6"));
        assertThat(settings.shopName()).isEqualTo("Lumen");
        mvc.perform(post("/admin/settings").with(asAdmin).with(csrf()).param("shopName", "Lumen").param("accentColor", "#00aa88")
                        .param("footerText", "Thanks").param("vatPercent", "90"))
                .andExpect(status().isOk());
    }

    @Test
    void onlyAdminsReachEmailAndSettingsPages() throws Exception {
        var asManager = user(new AppUserDetails(manager));
        mvc.perform(get("/admin/emails").with(asManager)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/settings").with(asManager)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/emails/reset").with(asManager).with(csrf()).param("subject", "S").param("heading", "H").param("body", "B"))
                .andExpect(status().isForbidden());
    }
}
