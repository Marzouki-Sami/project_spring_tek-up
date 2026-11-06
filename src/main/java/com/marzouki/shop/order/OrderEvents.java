package com.marzouki.shop.order;

public final class OrderEvents {

    private OrderEvents() {
    }

    public record OrderPlaced(Long orderId) {
    }

    public record OrderShipped(Long orderId) {
    }

    /** A product's stock has just dropped to the warning level. */
    public record LowStock(Long productId) {
    }
}