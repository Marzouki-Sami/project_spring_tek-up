package com.marzouki.shop.order;

import com.marzouki.shop.cart.Cart;
import com.marzouki.shop.cart.CartService;
import com.marzouki.shop.cart.CartView;
import com.marzouki.shop.catalog.ProductVariant;
import com.marzouki.shop.catalog.ProductVariantRepository;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.NotFoundException;
import com.marzouki.shop.order.dto.ShippingForm;
import com.marzouki.shop.user.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    /** At or below this many units a product triggers a low-stock alert. */
    public static final int LOW_STOCK_LEVEL = 5;

    private final OrderRepository orders;
    private final ProductVariantRepository variantRepository;
    private final CouponRepository coupons;
    private final StockMovementRepository movements;
    private final UserRepository users;
    private final CartService carts;
    private final org.springframework.context.ApplicationEventPublisher events;

    /**
     * Turns the basket into an order. Stock is taken line by line with atomic updates; if any line
     * cannot be filled the whole transaction rolls back and nothing is reserved.
     */
    @Transactional
    public Order place(Long userId, ShippingForm shipping, Cart cart) {
        CartView view = carts.view(cart);
        if (view.isEmpty()) {
            throw new ConflictException("Your cart is empty");
        }
        boolean changed = view.lines().stream().anyMatch(CartView.Line::limited)
                || (shipping.getExpectedTotal() != null && shipping.getExpectedTotal().compareTo(view.total()) != 0);
        if (changed) {
            throw new ConflictException("Prices or availability changed while you were checking out. "
                    + "Please review the updated total and confirm again.");
        }
        Coupon coupon = null;
        if (view.couponCode() != null) {
            coupon = coupons.findByCodeIgnoreCase(view.couponCode())
                    .orElseThrow(() -> new ConflictException("That coupon code is not valid"));
            if (coupons.claimUse(coupon.getId()) == 0) {
                throw new ConflictException("This coupon has been used up");
            }
        }

        Order order = new Order();
        order.setUser(users.getReferenceById(userId));
        order.setFullName(shipping.getFullName().trim());
        order.setAddress(shipping.getAddress().trim());
        order.setCity(shipping.getCity().trim());
        order.setPostalCode(shipping.getPostalCode().trim());
        order.setCountry(shipping.getCountry().trim());
        order.setCouponCode(view.couponCode());
        order.setSubtotal(view.subtotal());
        order.setDiscount(view.discount());
        order.setTotal(view.total());

        for (CartView.Line line : view.lines()) {
            ProductVariant variant = line.variant();
            if (variantRepository.takeStock(variant.getId(), line.quantity()) == 0) {
                throw new ConflictException("Sorry, " + line.product().getName() + " just went out of stock");
            }
            int before = variant.getStock();
            if (before > LOW_STOCK_LEVEL && before - line.quantity() <= LOW_STOCK_LEVEL) {
                events.publishEvent(new OrderEvents.LowStock(variant.getId()));
            }
            OrderItem item = new OrderItem();
            item.setProduct(line.product());
            item.setVariant(variant);
            item.setVariantLabel(variant.getLabel());
            item.setSku(variant.getSku());
            item.setName(line.product().getName());
            item.setUnitPrice(line.unitPrice());
            item.setQuantity(line.quantity());
            order.addItem(item);
        }
        orders.save(order);
        for (OrderItem item : order.getItems()) {
            movements.save(new StockMovement(item.getProduct().getId(), item.getVariant().getId(),
                    -item.getQuantity(), StockMovement.Reason.ORDER, order.getId()));
        }        events.publishEvent(new OrderEvents.OrderPlaced(order.getId()));
        return order;
    }

    public Order get(Long id) {
        return orders.findWithItemsById(id).orElseThrow(() -> new NotFoundException("Order", id));
    }

    /** An order as seen by its owner; other people's orders look like they do not exist. */
    public Order getOwned(Long id, Long userId) {
        Order order = get(id);
        if (!order.getUser().getId().equals(userId)) {
            throw new NotFoundException("Order", id);
        }
        return order;
    }

    public List<Order> forUser(Long userId) {
        return orders.findByUserIdOrderByCreatedAtDescIdDesc(userId);
    }

    public List<Order> all(OrderStatus status) {
        return status == null ? orders.findAllByOrderByCreatedAtDescIdDesc()
                : orders.findByStatusOrderByCreatedAtDescIdDesc(status);
    }

    @Transactional
    public Order cancelOwn(Long id, Long userId) {
        Order order = getOwned(id, userId);
        if (!order.getStatus().customerCanCancel()) {
            throw new ConflictException("This order is already being prepared. Contact the shop to change it.");
        }
        return moveTo(order, OrderStatus.CANCELLED);
    }

    @Transactional
    public Order changeStatus(Long id, OrderStatus target) {
        return moveTo(get(id), target);
    }

    private Order moveTo(Order order, OrderStatus target) {
        if (!order.getStatus().canMoveTo(target)) {
            throw new ConflictException("An order that is " + order.getStatus().getLabel().toLowerCase()
                    + " cannot become " + target.getLabel().toLowerCase());
        }
        if (orders.transition(order.getId(), order.getStatus(), target) == 0) {
            throw new ConflictException("This order was just changed by someone else. Reload the page and try again.");
        }
        if (target == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                if (item.getVariant() != null) {
                    variantRepository.returnStock(item.getVariant().getId(), item.getQuantity());
                    movements.save(new StockMovement(item.getProduct() == null ? null : item.getProduct().getId(),
                            item.getVariant().getId(), item.getQuantity(), StockMovement.Reason.CANCELLATION,
                            order.getId()));
                }
            }            if (order.getCouponCode() != null) {
                coupons.findByCodeIgnoreCase(order.getCouponCode()).ifPresent(c -> coupons.releaseUse(c.getId()));
            }
        }
        order.setStatus(target);
        if (target == OrderStatus.SHIPPED) {
            events.publishEvent(new OrderEvents.OrderShipped(order.getId()));
        }
        return order;
    }

    @Transactional
    public Coupon createCoupon(String code, int percentOff, Instant expiresAt, Integer maxUses) {
        String normalized = code.trim().toUpperCase();
        if (coupons.existsByCodeIgnoreCase(normalized)) {
            throw new ConflictException("The coupon code " + normalized + " already exists");
        }
        Coupon coupon = new Coupon();
        coupon.setCode(normalized);
        coupon.setPercentOff(percentOff);
        coupon.setExpiresAt(expiresAt);
        coupon.setMaxUses(maxUses);
        return coupons.save(coupon);
    }

    @Transactional
    public void setCouponActive(Long id, boolean active) {
        coupons.findById(id).orElseThrow(() -> new NotFoundException("Coupon", id)).setActive(active);
    }

    public List<Coupon> coupons() {
        return coupons.findAllByOrderByCreatedAtDescIdDesc();
    }
}