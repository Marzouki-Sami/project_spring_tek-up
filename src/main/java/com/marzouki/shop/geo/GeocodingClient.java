package com.marzouki.shop.geo;

import java.util.List;
import java.util.Optional;

/** Talks to whatever service finds addresses. Replaceable, for example in tests or with a paid provider. */
public interface GeocodingClient {

    List<GeoPlace> search(String query);

    Optional<GeoPlace> reverse(double lat, double lon);
}