package com.marzouki.shop.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param enabled whether mail is really sent over SMTP; when false messages are only written to the log
 * @param from    the sender shown to customers
 * @param baseUrl public address of the shop, used to build the links inside emails
 */
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(boolean enabled, String from, String baseUrl) {

    public MailProperties {
        from = from == null || from.isBlank() ? "Shop <no-reply@shop.local>" : from;
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "http://localhost:8081" : baseUrl.replaceAll("/+$", "");
    }
}