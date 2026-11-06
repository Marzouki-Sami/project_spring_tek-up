package com.marzouki.shop.common;

/** Thrown when a requested record does not exist. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String entity, Object id) {
        super(entity + " " + id + " was not found");
    }
}