package com.marzouki.shop.common;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Small building blocks for list filters. */
public final class Specs {

    private Specs() {
    }

    /** Matches when any of the (possibly dotted) text attributes contains the search text. Empty text matches all. */
    public static <T> Specification<T> text(String likePattern, String... attributes) {
        if (likePattern == null || likePattern.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            List<Predicate> any = new ArrayList<>();
            for (String attribute : attributes) {
                Path<String> path = null;
                for (String part : attribute.split("\\.")) {
                    path = path == null ? root.get(part) : path.get(part);
                }
                Expression<String> lower = cb.lower(path);
                any.add(cb.like(lower, likePattern, '\\'));
            }
            return cb.or(any.toArray(Predicate[]::new));
        };
    }

    public static <T> Specification<T> none() {
        return (root, query, cb) -> cb.conjunction();
    }
}