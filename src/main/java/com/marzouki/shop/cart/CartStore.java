package com.marzouki.shop.cart;

import com.marzouki.shop.user.UserRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps a signed-in customer's basket in the database. Guests only have the session basket; when a guest
 * signs in, the two are merged so nothing they added is lost.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartStore {

    private final SavedCartRepository saved;
    private final UserRepository users;
    private final CartService carts;

    /** The stored basket as a plain Cart, empty if the customer has none. */
    public Cart load(Long userId) {
        Cart cart = new Cart();
        saved.findByUserId(userId).ifPresent(s -> cart.replaceWith(s.getQuantities(), s.getCouponCode()));
        return cart;
    }

    @Transactional
    public void save(Long userId, Cart cart) {
        Map<Long, Integer> items = cart.snapshot();
        SavedCart stored = saved.findByUserId(userId).orElse(null);
        if (items.isEmpty() && cart.getCouponCode() == null) {
            if (stored != null) {
                saved.delete(stored);
            }
            return;
        }
        if (stored == null) {
            stored = new SavedCart();
            stored.setUser(users.getReferenceById(userId));
        } else if (stored.getQuantities().equals(items) && java.util.Objects.equals(stored.getCouponCode(), cart.getCouponCode())) {
            return;
        }
        stored.setQuantities(new LinkedHashMap<>(items));
        stored.setCouponCode(cart.getCouponCode());
        stored.setUpdatedAt(Instant.now());
        saved.save(stored);
    }

    @Transactional
    public void clear(Long userId) {
        saved.findByUserId(userId).ifPresent(saved::delete);
    }

    /**
     * Called when someone signs in: combines the guest basket with the stored one (quantities add up,
     * limited by stock), keeps the guest's coupon if they had one, and saves the result.
     */
    @Transactional
    public void signIn(Long userId, Cart sessionCart) {
        Cart stored = load(userId);
        Cart merged = new Cart();
        Map<Long, Integer> combined = new LinkedHashMap<>(stored.snapshot());
        sessionCart.snapshot().forEach((variantId, quantity) -> combined.merge(variantId, quantity, Integer::sum));
        combined.forEach((variantId, quantity) -> carts.setQuantity(merged, variantId, quantity));
        merged.setCouponCode(sessionCart.getCouponCode() != null ? sessionCart.getCouponCode() : stored.getCouponCode());
        sessionCart.replaceWith(merged.snapshot(), merged.getCouponCode());
        save(userId, sessionCart);
    }

    /** Baskets nobody has touched for two months are not worth keeping. */
    @Transactional
    public int deleteStale() {
        return saved.deleteStale(Instant.now().minusSeconds(60L * 24 * 3600));
    }
}