package com.marzouki.shop.geo;

/** One result of an address search: the pieces the address form needs, plus where it is. */
public record GeoPlace(String label, String street, String postalCode, String city, String country,
                       double lat, double lon) {
}