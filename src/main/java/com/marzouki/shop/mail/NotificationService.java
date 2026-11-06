package com.marzouki.shop.mail;

import com.marzouki.shop.catalog.Product;
import com.marzouki.shop.catalog.ProductRepository;
import com.marzouki.shop.order.Order;
import com.marzouki.shop.order.OrderEvents;
import com.marzouki.shop.order.OrderRepository;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserEvents;
import com.marzouki.shop.user.UserRepository;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends email in reaction to things that happened. Listeners run after the triggering transaction has
 * committed, so a mail is never sent for work that was rolled back, and a mail failure can never undo it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class NotificationService {

    private final MailComposer composer;
    private final MailOutbox outbox;
    private final UserRepository users;
    private final OrderRepository orders;
    private final ProductRepository products;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    void onVerification(UserEvents.VerificationRequested event) {
        users.findById(event.userId()).ifPresent(u -> deliver("verification", () -> composer.verification(u, event.token())));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    void onPasswordReset(UserEvents.PasswordResetRequested event) {
        users.findById(event.userId()).ifPresent(u -> deliver("password reset", () -> composer.passwordReset(u, event.token())));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    void onOrderPlaced(OrderEvents.OrderPlaced event) {
        orders.findWithItemsById(event.orderId())
                .ifPresent(o -> deliver("order confirmation", () -> composer.orderConfirmation(o)));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    void onOrderShipped(OrderEvents.OrderShipped event) {
        orders.findWithItemsById(event.orderId())
                .ifPresent(o -> deliver("shipping notice", () -> composer.orderShipped(o)));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    void onLowStock(OrderEvents.LowStock event) {
        Product product = products.findById(event.productId()).orElse(null);
        if (product == null) {
            return;
        }
        List<User> staff = users.findByRoleInAndEnabledTrue(List.of(Role.MANAGER, Role.ADMIN));
        for (User person : staff) {
            deliver("low stock alert", () -> composer.lowStock(person.getEmail(), product));
        }
    }

    private void deliver(String what, Supplier<Email> email) {
        try {
            outbox.send(email.get());
        } catch (RuntimeException e) {
            log.error("Could not prepare the {} email", what, e);
        }
    }
}