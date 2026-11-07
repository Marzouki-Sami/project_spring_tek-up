package com.marzouki.shop.mail;

import com.marzouki.shop.catalog.ProductVariant;
import com.marzouki.shop.order.Order;
import com.marzouki.shop.settings.ShopSettings;
import com.marzouki.shop.settings.SettingsService;
import com.marzouki.shop.user.User;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Builds each kind of email from its wording (built in, or edited by an admin) and the shop's branding.
 * The HTML comes from one fixed layout; the plain-text alternative is made from the same text.
 */
@Component
@RequiredArgsConstructor
public class MailComposer {

    private final SpringTemplateEngine templates;
    private final MailProperties properties;
    private final EmailTemplates wording;
    private final SettingsService settings;

    /** An email's text with the placeholders filled in. */
    public record Rendered(String subject, String heading, List<String> paragraphs, String buttonLabel) {
    }

    public Email verification(User user, String token) {
        return compose(EmailKind.VERIFY, user.getEmail(), Map.of("name", user.getFirstName()),
                properties.baseUrl() + "/verify-email?token=" + token, null);
    }

    public Email passwordReset(User user, String token) {
        return compose(EmailKind.RESET, user.getEmail(), Map.of("name", user.getFirstName()),
                properties.baseUrl() + "/reset-password?token=" + token, null);
    }

    public Email orderConfirmation(Order order) {
        return compose(EmailKind.ORDER_CONFIRMATION, order.getUser().getEmail(), orderValues(order),
                properties.baseUrl() + "/account/orders/" + order.getId(), order);
    }

    public Email orderShipped(Order order) {
        return compose(EmailKind.ORDER_SHIPPED, order.getUser().getEmail(), orderValues(order),
                properties.baseUrl() + "/account/orders/" + order.getId(), order);
    }

    public Email lowStock(String to, ProductVariant variant) {
        return compose(EmailKind.LOW_STOCK, to, Map.of("product", displayName(variant), "sku", variant.getSku(),
                "stock", String.valueOf(variant.getStock())), properties.baseUrl() + "/manage/variants/" + variant.getId() + "/stock", null);
    }

    public Email backInStock(User user, ProductVariant variant) {
        return compose(EmailKind.BACK_IN_STOCK, user.getEmail(), Map.of("name", user.getFirstName(), "product", displayName(variant)),
                properties.baseUrl() + "/products/" + variant.getProduct().getId(), null);
    }

    /** The email as it would look with sample data, using the given wording (saved or still being edited). */
    public Rendered preview(EmailKind kind, EmailTemplates.Wording text) {
        return render(kind, text, kind.sample(settings.shopName()));
    }

    public Email sample(EmailKind kind, String to) {
        return build(kind, wording.resolve(kind), to, kind.sample(settings.shopName()), properties.baseUrl() + "/", null);
    }

    public Email compose(EmailKind kind, String to, Map<String, String> values, String link, Order order) {
        Map<String, String> all = new HashMap<>(values);
        all.put("shop", settings.shopName());
        return build(kind, wording.resolve(kind), to, all, link, order);
    }

    private Email build(EmailKind kind, EmailTemplates.Wording text, String to, Map<String, String> values, String link, Order order) {
        Rendered rendered = render(kind, text, values);
        ShopSettings shop = settings.get();
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("shop", shop.getShopName());
        context.setVariable("accent", shop.getAccentColor());
        context.setVariable("footer", shop.getFooterText());
        context.setVariable("support", shop.getSupportEmail());
        context.setVariable("heading", rendered.heading());
        context.setVariable("paragraphs", rendered.paragraphs());
        context.setVariable("buttonLabel", rendered.buttonLabel());
        context.setVariable("link", link);
        context.setVariable("section", order == null ? "NONE" : kind.getSection().name());
        context.setVariable("order", order);

        StringBuilder plain = new StringBuilder(rendered.heading()).append("\n\n")
                .append(String.join("\n\n", rendered.paragraphs()));
        if (link != null && !rendered.buttonLabel().isBlank()) {
            plain.append("\n\n").append(rendered.buttonLabel()).append(": ").append(link);
        }
        plain.append("\n\n-- \n").append(shop.getShopName()).append("\n").append(shop.getFooterText());
        return new Email(to, rendered.subject(), plain.toString(), templates.process("mail/message", context));
    }

    private Rendered render(EmailKind kind, EmailTemplates.Wording text, Map<String, String> values) {
        return new Rendered(TemplateText.fill(text.subject(), values).replaceAll("[\\r\\n]+", " ").strip(),
                TemplateText.fill(text.heading(), values), TemplateText.paragraphs(TemplateText.fill(text.body(), values)),
                TemplateText.fill(text.buttonLabel() == null ? "" : text.buttonLabel(), values));
    }

    private static Map<String, String> orderValues(Order order) {
        return Map.of("name", order.getUser().getFirstName(), "orderNumber", String.valueOf(order.getId()),
                "total", money(order.getTotal()), "city", order.getCity());
    }

    private static String displayName(ProductVariant variant) {
        String label = variant.getLabel();
        String name = variant.getProduct().getName();
        return label == null ? name : name + " (" + label + ")";
    }

    private static String money(BigDecimal amount) {
        return String.format(Locale.ROOT, "%.2f €", amount);
    }
}