package com.marzouki.shop.common;

import java.util.Map;
import org.springframework.web.util.UriComponentsBuilder;

/** Builds links to other pages of a list that keep its filters. */
public record PageLinks(String path, Map<String, Object> params) {

    public String to(int page) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath(path);
        params.forEach(uri::queryParam);
        if (page > 0) {
            uri.queryParam("page", page);
        }
        return uri.build().encode().toUriString();
    }
}