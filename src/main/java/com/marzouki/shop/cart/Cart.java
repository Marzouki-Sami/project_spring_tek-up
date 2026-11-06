package com.marzouki.shop.cart;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

/**
 * The shopper's basket. It lives in the HTTP session, so a guest's basket disappears when the session
 * ends, while a signed-in shopper keeps it through sign-in (the session survives login).
 */
@Component
@SessionScope
public class Cart implements Serializable {

    public static final int MAX_PER_LINE = 20;

    private final Map<Long, Integer> quantities = new LinkedHashMap<>();
    private String couponCode;

    public synchronized Map<Long, Integer> snapshot() {
        return new LinkedHashMap<>(quantities);
    }

    public synchronized int quantityOf(Long productId) {
        return quantities.getOrDefault(productId, 0);
    }

    public synchronized void set(Long productId, int quantity) {
        if (quantity <= 0) {
            quantities.remove(productId);
        } else {
            quantities.put(productId, quantity);
        }
    }

    public synchronized void remove(Long productId) {
        quantities.remove(productId);
    }

    public synchronized int itemCount() {
        return quantities.values().stream().mapToInt(Integer::intValue).sum();
    }

    public synchronized String getCouponCode() {
        return couponCode;
    }

    public synchronized void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public synchronized void clear() {
        quantities.clear();
        couponCode = null;
    }
}