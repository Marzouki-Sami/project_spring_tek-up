package com.marzouki.shop.catalog;

import java.text.Normalizer;

/** Builds SKU-friendly codes from free text, such as "Linen shirt" becoming "LINEN-SHIRT". */
final class Skus {

    private Skus() {
    }

    static String fromText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD);
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < plain.length() && code.length() < maxLength; i++) {
            char ch = Character.toUpperCase(plain.charAt(i));
            if ((ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) {
                code.append(ch);
            } else if (Character.getType(ch) != Character.NON_SPACING_MARK && code.length() > 0
                    && code.charAt(code.length() - 1) != '-') {
                code.append('-');
            }
        }
        while (code.length() > 0 && code.charAt(code.length() - 1) == '-') {
            code.setLength(code.length() - 1);
        }
        return code.toString();
    }
}
