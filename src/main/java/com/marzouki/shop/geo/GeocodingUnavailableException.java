package com.marzouki.shop.geo;

public class GeocodingUnavailableException extends RuntimeException {

    public GeocodingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}