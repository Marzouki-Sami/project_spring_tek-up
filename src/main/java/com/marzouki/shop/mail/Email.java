package com.marzouki.shop.mail;

import java.util.List;

/** A finished message: plain-text and HTML bodies, and any files to attach. */
public record Email(String to, String subject, String text, String html, List<Attachment> attachments) {

    /** A file sent along with the message. */
    public record Attachment(String filename, String contentType, byte[] data) {
    }

    public Email(String to, String subject, String text, String html) {
        this(to, subject, text, html, List.of());
    }

    public Email withAttachment(Attachment attachment) {
        return new Email(to, subject, text, html, java.util.stream.Stream.concat(attachments.stream(), java.util.stream.Stream.of(attachment)).toList());
    }
}