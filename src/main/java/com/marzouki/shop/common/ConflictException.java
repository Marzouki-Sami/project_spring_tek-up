package com.marzouki.shop.common;

/** Thrown when an action conflicts with existing data, such as a duplicate SKU. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}