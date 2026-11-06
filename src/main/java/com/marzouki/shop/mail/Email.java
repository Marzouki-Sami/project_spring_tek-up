package com.marzouki.shop.mail;

public record Email(String to, String subject, String text, String html) {
}