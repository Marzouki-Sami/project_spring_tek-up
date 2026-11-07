package com.marzouki.shop.geo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** What the address pages need to know about the map, available in templates as {@code @geo}. */
@Component
@RequiredArgsConstructor
public class GeoView {

    private final GeoProperties properties;
    private final GeocodingService service;

    public boolean isEnabled() {
        return properties.enabled() && service.isAvailable();
    }

    public String getTileUrl() {
        return properties.tileUrl();
    }

    public String getAttribution() {
        return properties.attribution();
    }
}