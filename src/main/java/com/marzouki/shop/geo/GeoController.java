package com.marzouki.shop.geo;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Address search for the checkout and address book. It sits behind sign-in, so the shop's connection to
 * the provider cannot be used by anonymous visitors.
 */
@RestController
@RequestMapping("/geo")
@RequiredArgsConstructor
public class GeoController {

    private final GeocodingService geocoding;

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam("q") String query) {
        if (!geocoding.isAvailable()) {
            return unavailable();
        }
        String trimmed = query.trim();
        if (trimmed.length() < 3 || trimmed.length() > 200) {
            return ResponseEntity.badRequest().body(Map.of("error", "Type between 3 and 200 characters"));
        }
        try {
            List<GeoPlace> places = geocoding.search(trimmed);
            return ResponseEntity.ok(places);
        } catch (GeocodingUnavailableException e) {
            return unavailable();
        }
    }

    @GetMapping("/reverse")
    public ResponseEntity<?> reverse(@RequestParam double lat, @RequestParam double lon) {
        if (!geocoding.isAvailable()) {
            return unavailable();
        }
        if (Double.isNaN(lat) || Double.isNaN(lon) || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            return ResponseEntity.badRequest().body(Map.of("error", "That position is not valid"));
        }
        try {
            return geocoding.reverse(lat, lon).<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "No address was found at that spot")));
        } catch (GeocodingUnavailableException e) {
            return unavailable();
        }
    }

    private static ResponseEntity<?> unavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Address search is unavailable right now. You can still type the address."));
    }
}