package com.marzouki.shop.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marzouki.shop.address.Address;
import com.marzouki.shop.address.AddressService;
import com.marzouki.shop.api.dto.CatalogDtos.PageResponse;
import com.marzouki.shop.api.dto.OrderDtos.Line;
import com.marzouki.shop.api.dto.OrderDtos.OrderRequest;
import com.marzouki.shop.api.dto.OrderDtos.OrderResponse;
import com.marzouki.shop.api.dto.OrderDtos.OrderSummary;
import com.marzouki.shop.api.dto.OrderDtos.Shipping;
import com.marzouki.shop.cart.Cart;
import com.marzouki.shop.cart.CartService;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.ListQuery;
import com.marzouki.shop.order.IdempotencyKey;
import com.marzouki.shop.order.IdempotencyKeyRepository;
import com.marzouki.shop.order.Order;
import com.marzouki.shop.order.OrderService;
import com.marzouki.shop.order.OrderStatus;
import com.marzouki.shop.order.dto.ShippingForm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** What the order API does, returning plain response objects so nothing lazy leaks out of a transaction. */
@Service
@RequiredArgsConstructor
public class OrderApiService {

    /** Keys are remembered for a day; a retry after that is treated as a new request. */
    private static final long KEY_DAYS = 1;

    private final OrderService orders;
    private final CartService carts;
    private final AddressService addresses;
    private final IdempotencyKeyRepository keys;
    private final Validator validator;
    private final ObjectMapper json;
    private final com.marzouki.shop.mail.MailProperties mail;
    private final com.marzouki.shop.user.UserRepository userRepository;

    /** The order a request produced and whether it was an earlier one returned again. */
    public record Placed(Long orderId, boolean replayed) {
    }

    /** Places an order once per Idempotency-Key. The same key with the same body returns the same order. */
    @Transactional
    public Placed place(Long userId, String key, OrderRequest request) {
        String hash = hash(request);
        var existing = keys.findByUserIdAndKeyValue(userId, key);
        if (existing.isPresent()) {
            return replayOf(existing.get(), hash);
        }
        if (mail.enabled() && !userRepository.findById(userId).map(com.marzouki.shop.user.User::isEmailVerified).orElse(false)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "Confirm your email address before placing orders");
        }
        ShippingForm shipping = shippingFor(userId, request);
        Cart cart = new Cart();
        Map<Long, Integer> wanted = new LinkedHashMap<>();
        for (Line line : request.items()) {
            wanted.merge(line.variantId(), line.quantity(), Integer::sum);
        }
        wanted.forEach((variantId, quantity) -> {
            if (quantity > Cart.MAX_PER_LINE) {
                throw new ConflictException("At most " + Cart.MAX_PER_LINE + " of one item can be ordered at once");
            }
            cart.set(variantId, quantity);
        });
        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            carts.applyCoupon(cart, request.couponCode());
        }
        shipping.setExpectedTotal(request.expectedTotal());
        Order order = orders.place(userId, shipping, cart);
        keys.save(new IdempotencyKey(userId, key, hash, order.getId()));
        return new Placed(order.getId(), false);
    }

    /** Used when two requests with the same key arrive together and one lost the race. */
    @Transactional(readOnly = true)
    public Placed replay(Long userId, String key, OrderRequest request) {
        IdempotencyKey stored = keys.findByUserIdAndKeyValue(userId, key)
                .orElseThrow(() -> new ConflictException("The request could not be completed. Please try again."));
        return replayOf(stored, hash(request));
    }

    private Placed replayOf(IdempotencyKey stored, String hash) {
        if (!stored.getRequestHash().equals(hash)) {
            throw new ConflictException("This Idempotency-Key was already used with a different request");
        }
        return new Placed(stored.getOrderId(), true);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> mine(Long userId, ListQuery query) {
        return PageResponse.of(orders.search(query, userId), o -> OrderSummary.of(o, false));
    }

    @Transactional(readOnly = true)
    public OrderResponse mineById(Long userId, Long id) {
        return OrderResponse.of(orders.getOwned(id, userId), false);
    }

    @Transactional
    public OrderResponse cancel(Long userId, Long id) {
        return OrderResponse.of(orders.cancelOwn(id, userId), false);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> all(ListQuery query) {
        return PageResponse.of(orders.search(query), o -> OrderSummary.of(o, true));
    }

    @Transactional(readOnly = true)
    public OrderResponse byId(Long id) {
        return OrderResponse.of(orders.get(id), true);
    }

    @Transactional
    public OrderResponse changeStatus(Long id, OrderStatus status) {
        return OrderResponse.of(orders.changeStatus(id, status), true);
    }

    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void purgeOldKeys() {
        keys.deleteOlderThan(Instant.now().minus(KEY_DAYS, ChronoUnit.DAYS));
    }

    private ShippingForm shippingFor(Long userId, OrderRequest request) {
        Shipping in = request.shipping();
        ShippingForm form = new ShippingForm();
        if (in.addressId() != null) {
            Address chosen = addresses.get(in.addressId(), userId);
            form.setFullName(chosen.getFullName());
            form.setPhone(chosen.getPhone());
            form.setAddress(chosen.getStreet());
            form.setPostalCode(chosen.getPostalCode());
            form.setCity(chosen.getCity());
            form.setCountry(chosen.getCountry());
            form.setLatitude(chosen.getLatitude());
            form.setLongitude(chosen.getLongitude());
            return form;
        }
        form.setFullName(in.fullName());
        form.setPhone(in.phone());
        form.setAddress(in.address());
        form.setPostalCode(in.postalCode());
        form.setCity(in.city());
        form.setCountry(in.country());
        form.setLatitude(in.latitude());
        form.setLongitude(in.longitude());
        Set<ConstraintViolation<ShippingForm>> problems = validator.validate(form, ShippingForm.Manual.class);
        if (!problems.isEmpty()) {
            throw new ConstraintViolationException(problems);
        }
        return form;
    }

    private String hash(OrderRequest request) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException | JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}