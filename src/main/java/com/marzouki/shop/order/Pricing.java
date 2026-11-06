package com.marzouki.shop.order;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Pricing {

    private Pricing() {
    }

    /** The amount taken off for a percentage coupon, rounded to cents. */
    public static BigDecimal discount(BigDecimal subtotal, int percentOff) {
        return subtotal.multiply(BigDecimal.valueOf(percentOff))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}