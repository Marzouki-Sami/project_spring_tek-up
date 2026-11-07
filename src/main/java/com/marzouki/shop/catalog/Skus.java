package com.marzouki.shop.catalog;

import java.text.Normalizer;
import java.util.Locale;

/** Builds SKU-friendly codes from free text, such as "Linen shirt" becoming "LINEN-SHIRT". */
final class Skus {

    private Skus() {
    }

    static String fromText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String code = plain.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (code.length() > maxLength) {
            code = code.substring(0, maxLength).replaceAll("-+$", "");
        }
        return code;
    }
}
