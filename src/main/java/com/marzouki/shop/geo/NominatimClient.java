package com.marzouki.shop.geo;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Address search against a Nominatim-compatible service (OpenStreetMap by default). */
@Component
@ConditionalOnProperty(name = "app.geo.enabled", havingValue = "true", matchIfMissing = true)
class NominatimClient implements GeocodingClient {

    private final RestClient http;

    NominatimClient(GeoProperties properties) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.http = RestClient.builder().baseUrl(properties.url()).requestFactory(factory)
                .defaultHeader("User-Agent", properties.userAgent())
                .defaultHeader("Accept-Language", "en").build();
    }

    @Override
    public List<GeoPlace> search(String query) {
        try {
            JsonNode body = http.get().uri(u -> u.path("/search").queryParam("q", query).queryParam("format", "jsonv2")
                    .queryParam("addressdetails", 1).queryParam("limit", 5).build()).retrieve().body(JsonNode.class);
            List<GeoPlace> places = new ArrayList<>();
            if (body != null && body.isArray()) {
                body.forEach(node -> place(node).ifPresent(places::add));
            }
            return places;
        } catch (RuntimeException e) {
            throw new GeocodingUnavailableException("Address search is unavailable", e);
        }
    }

    @Override
    public Optional<GeoPlace> reverse(double lat, double lon) {
        try {
            JsonNode body = http.get().uri(u -> u.path("/reverse").queryParam("lat", lat).queryParam("lon", lon)
                    .queryParam("format", "jsonv2").queryParam("addressdetails", 1).build())
                    .retrieve().body(JsonNode.class);
            return body == null || body.has("error") ? Optional.empty() : place(body);
        } catch (RuntimeException e) {
            throw new GeocodingUnavailableException("Address search is unavailable", e);
        }
    }

    static Optional<GeoPlace> place(JsonNode node) {
        if (!node.hasNonNull("lat") || !node.hasNonNull("lon")) {
            return Optional.empty();
        }
        JsonNode address = node.path("address");
        String road = first(address, "road", "pedestrian", "footway", "path", "square");
        String number = text(address, "house_number");
        String street = road.isEmpty() ? first(address, "hamlet", "suburb", "neighbourhood")
                : (number.isEmpty() ? road : number + " " + road);
        String city = first(address, "city", "town", "village", "municipality", "county");
        try {
            return Optional.of(new GeoPlace(text(node, "display_name"), street, text(address, "postcode"), city,
                    text(address, "country"), Double.parseDouble(node.get("lat").asText()),
                    Double.parseDouble(node.get("lon").asText())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static String first(JsonNode node, String... names) {
        for (String name : names) {
            String value = text(node, name);
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static String text(JsonNode node, String name) {
        return node.hasNonNull(name) ? node.get(name).asText().trim() : "";
    }
}