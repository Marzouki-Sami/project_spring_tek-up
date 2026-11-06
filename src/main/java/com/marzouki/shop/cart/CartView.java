package com.marzouki.shop.cart;

import com.marzouki.shop.catalog.Product;
import java.math.BigDecimal;
import java.util.List;

/** What the cart page and checkout show: priced lines plus totals. */
public record CartView(List<Line> lines, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                       String couponCode, Integer percentOff) {

    public record Line(Product product, int quantity, BigDecimal lineTotal, boolean limited) {
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public int itemCount() {
        return lines.stream().mapToInt(Line::quantity).sum();
    }
}