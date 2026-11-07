package com.marzouki.shop.customer;

import java.math.BigDecimal;
import java.time.Instant;

/** One line of the customer list, with what the person has bought (cancelled orders do not count). */
public record CustomerRow(Long id, String firstName, String lastName, String email, boolean enabled, Instant joined,
                          long orders, BigDecimal spent, Instant lastOrder) {

    public String getFullName() {
        return firstName + " " + lastName;
    }
}