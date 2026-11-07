package com.marzouki.shop.common;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** One place that decides how amounts look, so pages, emails and invoices agree. */
public final class Money {

    private Money() {
    }

    /** For example 1199 becomes "1,199.00 €". */
    public static String format(BigDecimal amount) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return format.format(amount) + " €";
    }
}