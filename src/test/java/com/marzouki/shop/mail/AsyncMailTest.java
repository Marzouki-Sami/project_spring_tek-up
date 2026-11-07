package com.marzouki.shop.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.marzouki.shop.support.TestPasswords;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.UserRepository;
import com.marzouki.shop.user.UserService;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

/** In production mail leaves on its own thread pool, after the request has finished. */
@SpringBootTest
@Import(AsyncMailTest.Recording.class)
@TestPropertySource(properties = "app.mail.async=true")
class AsyncMailTest {

    static final List<String> THREADS = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class Recording {
        @Bean
        @Primary
        MailOutbox recordingOutbox() {
            return email -> THREADS.add(Thread.currentThread().getName());
        }
    }

    @Autowired UserService users;
    @Autowired UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void emailsAreSentOnTheMailThreadsNotTheRequestThread() {
        var form = new com.marzouki.shop.user.dto.RegistrationForm();
        form.setFirstName("Async");
        form.setLastName("Person");
        form.setEmail("async@example.com");
        String password = TestPasswords.strong();
        form.setPassword(password);
        form.setConfirmPassword(password);
        users.register(form);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(THREADS).isNotEmpty());
        assertThat(THREADS).allSatisfy(name -> assertThat(name).startsWith("mail-"));
        assertThat(Role.CUSTOMER).isNotNull();
    }
}
