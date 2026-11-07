package com.marzouki.shop.cart;

import com.marzouki.shop.catalog.Product;
import com.marzouki.shop.catalog.ProductVariant;
import java.math.BigDecimal;
import java.util.List;

/** What the cart page and checkout show: priced lines plus totals. */
public record CartView(List<Line> lines, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                       String couponCode, Integer percentOff) {

    public record Line(ProductVariant variant, int quantity, BigDecimal unitPrice, BigDecimal lineTotal,
                       boolean limited) {

        public Product product() {
            return variant.getProduct();
        }
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public int itemCount() {
        return lines.stream().mapToInt(Line::quantity).sum();
    }
}