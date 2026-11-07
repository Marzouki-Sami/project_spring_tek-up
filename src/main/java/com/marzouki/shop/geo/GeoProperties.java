package com.marzouki.shop.geo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the map and address search. The defaults use OpenStreetMap, which is free but asks for
 * light use; point {@code url} and {@code tileUrl} at your own or a commercial service for real traffic.
 *
 * @param enabled     turns the map and address search on or off (typing an address always works)
 * @param url         base address of a Nominatim-compatible search service
 * @param userAgent   identifies this shop to that service, as its usage policy requires
 * @param tileUrl     map tile template
 * @param attribution credit shown on the map
 */
@ConfigurationProperties(prefix = "app.geo")
public record GeoProperties(Boolean enabled, String url, String userAgent, String tileUrl, String attribution) {

    public GeoProperties {
        enabled = enabled == null || enabled;
        url = blank(url) ? "https://nominatim.openstreetmap.org" : url.replaceAll("/+$", "");
        userAgent = blank(userAgent) ? "SpringShop/1.0 (demo shop)" : userAgent;
        tileUrl = blank(tileUrl) ? "https://tile.openstreetmap.org/{z}/{x}/{y}.png" : tileUrl;
        attribution = blank(attribution) ? "&copy; OpenStreetMap contributors" : attribution;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}