package com.marzouki.shop.user;

/** Things other parts of the application react to, such as sending email, once the transaction commits. */
public final class UserEvents {

    private UserEvents() {
    }

    public record VerificationRequested(Long userId, String token) {
    }

    public record PasswordResetRequested(Long userId, String token) {
    }
}