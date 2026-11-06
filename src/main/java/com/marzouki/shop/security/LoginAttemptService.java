package com.marzouki.shop.security;

import com.marzouki.shop.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Slows down password guessing: after a number of wrong passwords the account refuses sign-in for a
 * while, whatever password is tried. A successful sign-in resets the counter.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UserRepository users;

    @Value("${app.security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.security.lock-duration:15m}")
    private Duration lockDuration;

    @EventListener
    @Transactional
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        users.findByEmailIgnoreCase(event.getAuthentication().getName().trim()).ifPresent(user -> {
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);
            if (attempts >= maxFailedAttempts) {
                user.setLockedUntil(Instant.now().plus(lockDuration));
                user.setFailedAttempts(0);
            }
        });
    }

    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof AppUserDetails details) {
            users.findById(details.getId()).ifPresent(user -> {
                if (user.getFailedAttempts() != 0 || user.getLockedUntil() != null) {
                    user.setFailedAttempts(0);
                    user.setLockedUntil(null);
                }
            });
        }
    }
}