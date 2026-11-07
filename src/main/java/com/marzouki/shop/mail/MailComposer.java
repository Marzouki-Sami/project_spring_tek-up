package com.marzouki.shop.mail;

import com.marzouki.shop.catalog.Product;
import com.marzouki.shop.catalog.ProductVariant;
import com.marzouki.shop.order.Order;
import com.marzouki.shop.user.User;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/** Builds each kind of email: an HTML body from a template plus a plain-text alternative. */
@Component
@RequiredArgsConstructor
public class MailComposer {

    private final SpringTemplateEngine templates;
    private final MailProperties properties;

    public Email verification(User user, String token) {
        String link = properties.baseUrl() + "/verify-email?token=" + token;
        return build(user.getEmail(), "Confirm your email address", "verify",
                Map.of("name", user.getFirstName(), "link", link),
                "Hi " + user.getFirstName() + ",\n\nConfirm your email address to finish setting up your account:\n"
                        + link + "\n\nThe link works for 2 days. If you did not sign up, ignore this message.");
    }

    public Email passwordReset(User user, String token) {
        String link = properties.baseUrl() + "/reset-password?token=" + token;
        return build(user.getEmail(), "Reset your password", "reset",
                Map.of("name", user.getFirstName(), "link", link),
                "Hi " + user.getFirstName() + ",\n\nChoose a new password here:\n" + link
                        + "\n\nThe link works for 1 hour. If you did not ask for this, ignore this message; "
                        + "your password stays the same.");
    }

    public Email orderConfirmation(Order order) {
        String link = properties.baseUrl() + "/account/orders/" + order.getId();
        return build(order.getUser().getEmail(), "Your order #" + order.getId() + " is confirmed", "order-confirmation",
                Map.of("order", order, "name", order.getUser().getFirstName(), "link", link),
                "Hi " + order.getUser().getFirstName() + ",\n\nThanks for your order #" + order.getId()
                        + ". Total: " + order.getTotal() + " EUR.\nFollow it here: " + link);
    }

    public Email orderShipped(Order order) {
        String link = properties.baseUrl() + "/account/orders/" + order.getId();
        return build(order.getUser().getEmail(), "Your order #" + order.getId() + " is on its way", "order-shipped",
                Map.of("order", order, "name", order.getUser().getFirstName(), "link", link),
                "Hi " + order.getUser().getFirstName() + ",\n\nGood news: order #" + order.getId()
                        + " has shipped to " + order.getCity() + ".\nDetails: " + link);
    }

    public Email lowStock(String to, ProductVariant variant) {
        Product product = variant.getProduct();
        String link = properties.baseUrl() + "/manage/variants/" + variant.getId() + "/stock";
        String name = variant.getLabel() == null ? product.getName() : product.getName() + " (" + variant.getLabel() + ")";
        return build(to, "Low stock: " + name, "low-stock",
                Map.of("name", name, "sku", variant.getSku(), "stock", variant.getStock(), "link", link),
                name + " (" + variant.getSku() + ") is down to " + variant.getStock() + " units.\nRestock: " + link);
    }
    private Email build(String to, String subject, String template, Map<String, Object> variables, String text) {
        Context context = new Context(Locale.ENGLISH, variables);
        return new Email(to, subject, text, templates.process("mail/" + template, context));
    }
}