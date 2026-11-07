package com.marzouki.shop.mail;

import static org.assertj.core.api.Assertions.assertThat;

import com.marzouki.shop.support.TestPasswords;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserRepository;
import com.marzouki.shop.user.UserService;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

/** The cooldown that stops the sign-up and reset forms being used to flood an address, and staff alert opt-out. */
@SpringBootTest
@Import(EmailCooldownTest.Recording.class)
@TestPropertySource(properties = "app.mail.cooldown=60s")
class EmailCooldownTest {

    static final List<Email> SENT = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class Recording {
        @Bean
        @Primary
        MailOutbox recordingOutbox() {
            return SENT::add;
        }
    }

    @Autowired UserService users;
    @Autowired UserRepository userRepository;

    User person;

    @BeforeEach
    void data() {
        SENT.clear();
        person = users.create("Pat", "Person", "pat@example.com", TestPasswords.strong(), Role.CUSTOMER);
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void repeatedRequestsToOneAddressSendOnlyOneEmailPerKind() {
        User pat = users.create("Pat", "Reset", "reset-pat@example.com", TestPasswords.strong(), Role.CUSTOMER);
        users.requestPasswordReset("reset-pat@example.com");
        users.requestPasswordReset("RESET-PAT@example.com");
        users.requestPasswordReset("reset-pat@example.com");
        assertThat(SENT).hasSize(1);
        assertThat(pat.getEmail()).isEqualTo("reset-pat@example.com");
    }

    @Test
    void confirmationEmailsAreThrottledToo() {
        var form = new com.marzouki.shop.user.dto.RegistrationForm();
        form.setFirstName("Una");
        form.setLastName("New");
        form.setEmail("una@example.com");
        String password = TestPasswords.strong();
        form.setPassword(password);
        form.setConfirmPassword(password);
        User una = users.register(form);
        assertThat(SENT).hasSize(1);
        users.resendVerification(una.getId());
        users.resendVerification(una.getId());
        assertThat(SENT).hasSize(1);
    }

    @Test
    void otherAddressesAreNotAffected() {
        users.create("Quinn", "Other", "quinn2@example.com", TestPasswords.strong(), Role.CUSTOMER);
        users.create("Rae", "Other", "rae2@example.com", TestPasswords.strong(), Role.CUSTOMER);
        users.requestPasswordReset("quinn2@example.com");
        users.requestPasswordReset("rae2@example.com");
        assertThat(SENT).hasSize(2);
    }
    @Test
    void staffCanSwitchLowStockEmailsOff() {
        User manager = users.create("Mo", "Manager", "mo@example.com", TestPasswords.strong(), Role.MANAGER);
        assertThat(userRepository.findByRoleInAndEnabledTrueAndLowStockAlertsTrue(List.of(Role.MANAGER, Role.ADMIN)))
                .extracting(User::getEmail).containsExactly("mo@example.com");
        var form = users.profileFor(manager.getId());
        form.setLowStockAlerts(false);
        users.updateProfile(manager.getId(), form);
        assertThat(userRepository.findByRoleInAndEnabledTrueAndLowStockAlertsTrue(List.of(Role.MANAGER, Role.ADMIN))).isEmpty();

        var customerForm = users.profileFor(person.getId());
        customerForm.setLowStockAlerts(false);
        users.updateProfile(person.getId(), customerForm);
        assertThat(userRepository.findById(person.getId()).orElseThrow().isLowStockAlerts()).isTrue();
    }
}
