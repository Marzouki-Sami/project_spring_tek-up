package com.marzouki.shop.catalog;

import com.marzouki.shop.common.Like;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The customer's choices on the shop page. Invalid values are ignored rather than rejected. */
public class ShopQuery {

    public static final int PAGE_SIZE = 12;
    public static final List<String> SORTS = List.of("name", "price-asc", "price-desc", "newest", "rating");

    private String q = "";
    private Long category;
    private Long subcategory;
    private String minPrice = "";
    private String maxPrice = "";
    private boolean inStock;
    private int minRating;
    private String sort = "name";
    private int page;

    public String getQ() {
        String text = q == null ? "" : q.trim();
        return text.length() > 80 ? text.substring(0, 80) : text;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public Long getCategory() {
        return category;
    }

    public void setCategory(Long category) {
        this.category = category;
    }

    public Long getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(Long subcategory) {
        this.subcategory = subcategory;
    }

    public String getMinPrice() {
        return minPrice == null ? "" : minPrice.trim();
    }

    public void setMinPrice(String minPrice) {
        this.minPrice = minPrice;
    }

    public String getMaxPrice() {
        return maxPrice == null ? "" : maxPrice.trim();
    }

    public void setMaxPrice(String maxPrice) {
        this.maxPrice = maxPrice;
    }

    public boolean isInStock() {
        return inStock;
    }

    public void setInStock(boolean inStock) {
        this.inStock = inStock;
    }

    /** 0 for no filter, otherwise 1 to 5 stars and up. */
    public int getMinRating() {
        return minRating >= 1 && minRating <= 5 ? minRating : 0;
    }

    public void setMinRating(int minRating) {
        this.minRating = minRating;
    }

    public String getSort() {
        return sort != null && SORTS.contains(sort) ? sort : "name";
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

    public BigDecimal minPriceValue() {
        return money(getMinPrice());
    }

    public BigDecimal maxPriceValue() {
        return money(getMaxPrice());
    }

    String likePattern() {
        return Like.contains(getQ());
    }

    private static BigDecimal money(String text) {
        try {
            BigDecimal value = text.isEmpty() ? null : new BigDecimal(text.replace(',', '.'));
            return value == null || value.signum() < 0 || value.compareTo(new BigDecimal("99999999")) > 0 ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** True when any filter other than the sort is active, so the page can offer a way to clear them. */
    public boolean isFiltered() {
        return !getQ().isEmpty() || category != null || subcategory != null || minPriceValue() != null
                || maxPriceValue() != null || inStock || getMinRating() > 0;
    }

    public Map<String, Object> params() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (!getQ().isEmpty()) {
            map.put("q", getQ());
        }
        if (category != null) {
            map.put("category", category);
        }
        if (subcategory != null) {
            map.put("subcategory", subcategory);
        }
        if (minPriceValue() != null) {
            map.put("minPrice", minPriceValue().toPlainString());
        }
        if (maxPriceValue() != null) {
            map.put("maxPrice", maxPriceValue().toPlainString());
        }
        if (inStock) {
            map.put("inStock", true);
        }
        if (getMinRating() > 0) {
            map.put("minRating", getMinRating());
        }
        if (!getSort().equals("name")) {
            map.put("sort", getSort());
        }
        return map;
    }
}