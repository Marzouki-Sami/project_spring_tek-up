package com.marzouki.shop.common;

import java.util.Locale;

/** Builds SQL LIKE patterns from user input with the wildcard characters made literal. */
public final class Like {

    private Like() {
    }

    /** A lower-case "contains" pattern for use with {@code escape '\'}, or an empty string for no search. */
    public static String contains(String text) {
        String clean = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (clean.isEmpty()) {
            return "";
        }
        if (clean.length() > 80) {
            clean = clean.substring(0, 80);
        }
        return "%" + clean.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}