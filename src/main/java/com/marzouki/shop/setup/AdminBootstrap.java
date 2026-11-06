package com.marzouki.shop.setup;

import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.UserRepository;
import com.marzouki.shop.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Creates the first administrator from configuration, so a fresh install is never locked out. */
@Slf4j
@Component
public class AdminBootstrap implements ApplicationRunner {

    private final UserService users;
    private final UserRepository repository;
    private final String email;
    private final String password;

    AdminBootstrap(UserService users, UserRepository repository,
                   @Value("${app.bootstrap.admin-email:}") String email,
                   @Value("${app.bootstrap.admin-password:}") String password) {
        this.users = users;
        this.repository = repository;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        if (repository.existsByEmailIgnoreCase(email)) {
            return;
        }
        users.create("Shop", "Administrator", email, password, Role.ADMIN);
        log.info("Created the initial administrator {}", email);
    }
}