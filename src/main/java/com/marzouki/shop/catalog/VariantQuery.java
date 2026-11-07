package com.marzouki.shop.catalog;

import java.util.List;
import java.util.Locale;

/** What the variants table is showing: a search text, a status filter and a page. Bound from request parameters. */
public class VariantQuery {

    public enum Status { ALL, ACTIVE, INACTIVE, LOW, OUT }

    public static final List<Integer> SIZES = List.of(10, 25, 50, 100);
    public static final int DEFAULT_SIZE = 25;

    private String q = "";
    private String status = "ALL";
    private int page;
    private int size = DEFAULT_SIZE;

    public String getQ() {
        String text = q == null ? "" : q.trim();
        return text.length() > 80 ? text.substring(0, 80) : text;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public Status getStatus() {
        try {
            return Status.valueOf(status == null ? "ALL" : status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Status.ALL;
        }
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPage() {
        return Math.max(page, 0);
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return SIZES.contains(size) ? size : DEFAULT_SIZE;
    }

    public void setSize(int size) {
        this.size = size;
    }

    /** The same query pointing at another page. */
    public VariantQuery onPage(int target) {
        VariantQuery copy = new VariantQuery();
        copy.q = getQ();
        copy.status = getStatus().name();
        copy.size = getSize();
        copy.page = target;
        return copy;
    }

    /** The search text as a SQL LIKE pattern with wildcards escaped, or an empty string for no search. */
    String likePattern() {
        String text = getQ().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return "";
        }
        return "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
