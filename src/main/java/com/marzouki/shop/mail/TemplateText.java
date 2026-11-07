package com.marzouki.shop.mail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The tiny template language of editable emails: plain text with {{placeholders}}. Nothing is executed and
 * no markup is understood, so an edited email can never run code or inject HTML. Values are escaped when
 * the page is built.
 */
final class TemplateText {

    private static final Pattern TOKEN = Pattern.compile("\\{\\{\\s*([A-Za-z]{1,30})\\s*}}");

    private TemplateText() {
    }

    static String fill(String text, Map<String, String> values) {
        Matcher matcher = TOKEN.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String value = values.getOrDefault(matcher.group(1), "");
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** Placeholders used in the text that are not in the allowed list. */
    static Set<String> unknown(String text, Collection<String> allowed) {
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = TOKEN.matcher(text);
        while (matcher.find()) {
            if (!allowed.contains(matcher.group(1))) {
                found.add(matcher.group(1));
            }
        }
        return found;
    }

    /** Whether the text has a "{{" that is not a well-formed placeholder, such as {{name or {{ 1 }}. */
    static boolean hasBrokenPlaceholder(String text) {
        String stripped = TOKEN.matcher(text).replaceAll("");
        return stripped.contains("{{") || stripped.contains("}}");
    }

    static List<String> paragraphs(String text) {
        List<String> paragraphs = new ArrayList<>();
        for (String block : text.replace("\r\n", "\n").split("\n\\s*\n")) {
            String trimmed = block.strip();
            if (!trimmed.isEmpty()) {
                paragraphs.add(trimmed);
            }
        }
        return paragraphs;
    }
}