package com.marzouki.shop.seo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param url the public address of the shop, used for canonical links, social previews and the sitemap */
@ConfigurationProperties(prefix = "app.site")
public record SiteProperties(String url) {

    public SiteProperties {
        url = url == null || url.isBlank() ? "http://localhost:8081" : url.strip().replaceAll("/+$", "");
    }
}