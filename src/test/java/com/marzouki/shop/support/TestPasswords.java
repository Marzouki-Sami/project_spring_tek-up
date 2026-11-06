package com.marzouki.shop.support;

import java.util.UUID;

/** Passwords generated per test run, so no credential-looking literals live in the repository. */
public final class TestPasswords {

    private TestPasswords() {
    }

    /** Satisfies the password policy: upper, lower, digit, symbol, long enough. */
    public static String strong() {
        return "Aa1!" + UUID.randomUUID().toString().replace("-", "");
    }
}