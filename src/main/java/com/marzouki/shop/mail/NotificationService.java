package com.marzouki.shop.mail;

import com.marzouki.shop.catalog.ProductVariant;
import com.marzouki.shop.catalog.ProductVariantRepository;
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
    private final ProductVariantRepository variantRepository;
    private final SendThrottle throttle;
    private final com.marzouki.shop.invoice.InvoiceService invoices;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    @org.springframework.scheduling.annotation.Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    void onVerification(UserEvents.VerificationRequested event) {
        users.findById(event.userId()).filter(u -> throttle.allow("verify", u.getEmail()))
                .ifPresent(u -> deliver("verification", () -> composer.verification(u, event.token())));
    }

    @org.springframework.scheduling.annotation.Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    void onPasswordReset(UserEvents.PasswordResetRequested event) {
        users.findById(event.userId()).filter(u -> throttle.allow("reset", u.getEmail()))
                .ifPresent(u -> deliver("password reset", () -> composer.passwordReset(u, event.token())));
    }

    @org.springframework.scheduling.annotation.Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onOrderPlaced(OrderEvents.OrderPlaced event) {
        orders.findWithItemsById(event.orderId())
                .ifPresent(o -> deliver("order confirmation", () -> withInvoice(composer.orderConfirmation(o), o.getId())));
    }

    @org.springframework.scheduling.annotation.Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    void onOrderShipped(OrderEvents.OrderShipped event) {
        orders.findWithItemsById(event.orderId())
                .ifPresent(o -> deliver("shipping notice", () -> composer.orderShipped(o)));
    }

    @org.springframework.scheduling.annotation.Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    void onLowStock(OrderEvents.LowStock event) {
        ProductVariant variant = variantRepository.findWithDetailsById(event.variantId()).orElse(null);
        if (variant == null) {
            return;
        }
        List<User> staff = users.findByRoleInAndEnabledTrueAndLowStockAlertsTrue(List.of(Role.MANAGER, Role.ADMIN));
        for (User person : staff) {
            deliver("low stock alert", () -> composer.lowStock(person.getEmail(), variant));
        }
    }

    /** Tells people who were waiting for an item that it is available again, at most once a week per item. */
    @org.springframework.scheduling.annotation.Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void onBackInStock(OrderEvents.BackInStock event) {
        ProductVariant variant = variantRepository.findWithDetailsById(event.variantId()).orElse(null);
        if (variant == null || !variant.isActive() || !variant.getProduct().isActive() || variant.getStock() <= 0) {
            return;
        }
        for (User person : users.backInStockAudience(variant.getId(), variant.getProduct().getId())) {
            if (claimAlert(person.getId(), variant.getId())) {
                deliver("back in stock alert", () -> composer.backInStock(person, variant));
            }
        }
    }

    /** True when this person has not been told about this item in the last week. Safe when two alerts race. */
    private boolean claimAlert(Long userId, Long variantId) {
        java.sql.Timestamp now = java.sql.Timestamp.from(java.time.Instant.now());
        java.sql.Timestamp cutoff = java.sql.Timestamp.from(java.time.Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS));
        if (jdbc.update("update stock_alert_log set sent_at = ? where user_id = ? and variant_id = ? and sent_at < ?", now, userId, variantId, cutoff) > 0) {
            return true;
        }
        try {
            return jdbc.update("insert into stock_alert_log (user_id, variant_id, sent_at) values (?, ?, ?)", userId, variantId, now) == 1;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            return false;
        }
    }

    /** Attaches the invoice PDF. If it cannot be made the confirmation still goes out without it. */
    private Email withInvoice(Email email, Long orderId) {
        try {
            var document = invoices.document(orderId);
            return email.withAttachment(new Email.Attachment(document.filename(), "application/pdf", document.pdf()));
        } catch (RuntimeException e) {
            log.error("Could not attach the invoice to the confirmation of order {}", orderId, e);
            return email;
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