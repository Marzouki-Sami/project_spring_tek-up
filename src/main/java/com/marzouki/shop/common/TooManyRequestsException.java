package com.marzouki.shop.common;

/** Too many attempts in a short time. It is a conflict for pages that handle it themselves, and HTTP 429 for the API. */
public class TooManyRequestsException extends ConflictException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}