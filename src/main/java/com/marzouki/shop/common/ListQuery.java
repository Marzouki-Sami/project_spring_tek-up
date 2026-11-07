package com.marzouki.shop.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * What a back-office list is showing: a search text, a status filter, a sort and a page. Bound from request
 * parameters; anything invalid falls back to a default instead of failing, and the page size is capped.
 */
public class ListQuery {

    public static final List<Integer> SIZES = List.of(10, 25, 50, 100);
    public static final int DEFAULT_SIZE = 25;

    private String q = "";
    private String status = "";
    private String sort = "";
    private int page;
    private int size = DEFAULT_SIZE;

    public String getQ() {
        String text = q == null ? "" : q.trim();
        return text.length() > 80 ? text.substring(0, 80) : text;
    }

    public void setQ(String q) {
        this.q = q;
    }

    /** Upper-case status key, or an empty string for "all". */
    public String getStatus() {
        String value = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        return value.length() > 20 || value.equals("ALL") ? "" : value;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /** Lower-case sort key, or an empty string for the list's default. */
    public String getSort() {
        String value = sort == null ? "" : sort.trim().toLowerCase(Locale.ROOT);
        return value.length() > 20 ? "" : value;
    }

    public void setSort(String sort) {
        this.sort = sort;
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

    /** Ordered value-to-label pairs for a filter or sort dropdown: choices("", "All", "ACTIVE", "Active"). */
    public static Map<String, String> choices(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

    public List<Integer> getSizes() {
        return SIZES;
    }

    public String like() {
        return Like.contains(getQ());
    }

    public PageRequest pageRequest(Sort sort) {
        return PageRequest.of(getPage(), getSize(), sort);
    }

    /** The values that differ from the defaults, for building links that keep the current view. */
    public Map<String, Object> params() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (!getQ().isEmpty()) {
            map.put("q", getQ());
        }
        if (!getStatus().isEmpty()) {
            map.put("status", getStatus());
        }
        if (!getSort().isEmpty()) {
            map.put("sort", getSort());
        }
        if (getSize() != DEFAULT_SIZE) {
            map.put("size", getSize());
        }
        return map;
    }
}