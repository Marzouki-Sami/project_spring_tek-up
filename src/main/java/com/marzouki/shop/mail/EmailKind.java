package com.marzouki.shop.mail;

import java.util.List;
import java.util.Map;

/**
 * Every email the shop sends. Each has built-in wording that an admin can edit; the placeholders listed
 * for a kind are the only ones it may use.
 */
public enum EmailKind {

    VERIFY("Confirm email address", "Sent after sign-up so the customer can confirm their address.",
            List.of("name", "shop"), Section.NONE,
            "Confirm your email address", "Welcome, {{name}}!",
            "Please confirm your email address to finish setting up your account.\n\n"
                    + "The link works for 2 days. If you did not sign up, you can ignore this message.",
            "Confirm email address"),

    RESET("Password reset", "Sent when someone asks to reset their password.",
            List.of("name", "shop"), Section.NONE,
            "Reset your password", "Reset your password",
            "Hi {{name}},\n\nChoose a new password with the button below.\n\n"
                    + "The link works for 1 hour. If you did not ask for this, ignore this message; your password stays the same.",
            "Choose a new password"),

    ORDER_CONFIRMATION("Order confirmation", "Sent when an order is placed. The invoice is attached when invoices are on.",
            List.of("name", "shop", "orderNumber", "total", "city"), Section.LINES,
            "Your order #{{orderNumber}} is confirmed", "Thank you for your order",
            "Hi {{name}},\n\nThank you for your order #{{orderNumber}}. We will let you know when it ships.",
            "View your order"),

    ORDER_SHIPPED("Order shipped", "Sent when a manager marks an order as shipped.",
            List.of("name", "shop", "orderNumber", "total", "city"), Section.ADDRESS,
            "Your order #{{orderNumber}} is on its way", "Your order is on its way",
            "Hi {{name}},\n\nGood news: order #{{orderNumber}} has shipped to {{city}}.",
            "View your order"),

    LOW_STOCK("Low stock alert", "Sent to managers and admins who have alerts on when an item runs low.",
            List.of("product", "sku", "stock", "shop"), Section.NONE,
            "Low stock: {{product}}", "Low stock",
            "{{product}} ({{sku}}) is down to {{stock}} units.",
            "Restock now"),

    BACK_IN_STOCK("Back in stock", "Sent to a customer when something in their basket or wishlist is available again.",
            List.of("name", "shop", "product"), Section.NONE,
            "{{product}} is back in stock", "Back in stock",
            "Hi {{name}},\n\nGood news: {{product}} is available again. Stock can go quickly, so take a look soon.",
            "View the product");

    /** Extra order details shown under the message. */
    public enum Section { NONE, LINES, ADDRESS }

    private final String label;
    private final String description;
    private final List<String> placeholders;
    private final Section section;
    private final String subject;
    private final String heading;
    private final String body;
    private final String button;

    EmailKind(String label, String description, List<String> placeholders, Section section, String subject,
              String heading, String body, String button) {
        this.label = label;
        this.description = description;
        this.placeholders = placeholders;
        this.section = section;
        this.subject = subject;
        this.heading = heading;
        this.body = body;
        this.button = button;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getPlaceholders() {
        return placeholders;
    }

    public Section getSection() {
        return section;
    }

    public String getDefaultSubject() {
        return subject;
    }

    public String getDefaultHeading() {
        return heading;
    }

    public String getDefaultBody() {
        return body;
    }

    public String getDefaultButton() {
        return button;
    }

    /** The key in the URL and the database. */
    public String getKey() {
        return name().toLowerCase().replace('_', '-');
    }

    public static EmailKind ofKey(String key) {
        for (EmailKind kind : values()) {
            if (kind.getKey().equals(key)) {
                return kind;
            }
        }
        throw new com.marzouki.shop.common.NotFoundException("Email", key);
    }

    /** Values shown in previews and test messages. */
    public Map<String, String> sample(String shopName) {
        return Map.of("name", "Alex", "shop", shopName, "orderNumber", "1042", "total", "59.90 €", "city", "Lyon",
                "product", "Classic Cotton Tee (M / Black)", "sku", "TEE-001-M-BLACK", "stock", "3");
    }
}