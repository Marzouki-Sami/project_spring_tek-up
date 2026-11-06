package com.marzouki.shop.cart;

import com.marzouki.shop.catalog.Product;
import com.marzouki.shop.catalog.ProductRepository;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.order.Coupon;
import com.marzouki.shop.order.CouponRepository;
import com.marzouki.shop.order.Pricing;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final ProductRepository products;
    private final CouponRepository coupons;

    /** Adds to the basket, never beyond the stock on hand or the per-line limit. */
    public void add(Cart cart, Long productId, int quantity) {
        Product product = products.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ConflictException("This product is no longer available"));
        if (product.getStock() == 0) {
            throw new ConflictException(product.getName() + " is sold out");
        }
        int wanted = cart.quantityOf(productId) + Math.max(quantity, 1);
        cart.set(productId, Math.min(wanted, Math.min(product.getStock(), Cart.MAX_PER_LINE)));
    }

    public void setQuantity(Cart cart, Long productId, int quantity) {
        if (quantity <= 0) {
            cart.remove(productId);
            return;
        }
        Product product = products.findByIdAndActiveTrue(productId).orElse(null);
        if (product == null) {
            cart.remove(productId);
            return;
        }
        cart.set(productId, Math.min(quantity, Math.min(product.getStock(), Cart.MAX_PER_LINE)));
    }

    @Transactional
    public void applyCoupon(Cart cart, String code) {
        Coupon coupon = coupons.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ConflictException("That coupon code is not valid"));
        String problem = coupon.problemAt(Instant.now());
        if (problem != null) {
            throw new ConflictException(problem);
        }
        cart.setCouponCode(coupon.getCode());
    }

    /** Prices the basket with today's data: removed products vanish, quantities shrink to the stock. */
    public CartView view(Cart cart) {
        Map<Long, Integer> wanted = cart.snapshot();
        Map<Long, Product> found = products.findByIdIn(wanted.keySet()).stream()
                .filter(Product::isActive)
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<CartView.Line> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : wanted.entrySet()) {
            Product product = found.get(entry.getKey());
            if (product == null || product.getStock() == 0) {
                cart.remove(entry.getKey());
                continue;
            }
            int quantity = Math.min(entry.getValue(), product.getStock());
            if (quantity != entry.getValue()) {
                cart.set(entry.getKey(), quantity);
            }
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            lines.add(new CartView.Line(product, quantity, lineTotal, quantity < entry.getValue()));
            subtotal = subtotal.add(lineTotal);
        }

        Coupon coupon = cart.getCouponCode() == null ? null
                : coupons.findByCodeIgnoreCase(cart.getCouponCode()).orElse(null);
        if (coupon != null && coupon.problemAt(Instant.now()) != null) {
            cart.setCouponCode(null);
            coupon = null;
        }
        BigDecimal discount = coupon == null ? BigDecimal.ZERO : Pricing.discount(subtotal, coupon.getPercentOff());
        return new CartView(lines, subtotal, discount, subtotal.subtract(discount),
                coupon == null ? null : coupon.getCode(), coupon == null ? null : coupon.getPercentOff());
    }
}