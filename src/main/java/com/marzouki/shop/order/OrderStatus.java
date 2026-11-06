package com.marzouki.shop.order;

import java.util.Set;

public enum OrderStatus {
    PLACED("Placed", "primary"),
    PROCESSING("Processing", "info"),
    SHIPPED("Shipped", "warning"),
    DELIVERED("Delivered", "success"),
    CANCELLED("Cancelled", "secondary");

    private final String label;
    private final String color;

    OrderStatus(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }

    /** The statuses an order in this status may move to. */
    public Set<OrderStatus> next() {
        return switch (this) {
            case PLACED -> Set.of(PROCESSING, CANCELLED);
            case PROCESSING -> Set.of(SHIPPED, CANCELLED);
            case SHIPPED -> Set.of(DELIVERED);
            case DELIVERED, CANCELLED -> Set.of();
        };
    }

    public boolean canMoveTo(OrderStatus target) {
        return next().contains(target);
    }

    /** Customers may cancel on their own only before the order is being prepared. */
    public boolean customerCanCancel() {
        return this == PLACED;
    }
}