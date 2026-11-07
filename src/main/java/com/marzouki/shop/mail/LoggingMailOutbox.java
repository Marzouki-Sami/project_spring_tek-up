package com.marzouki.shop.mail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development default: instead of sending, writes each message to a text file so confirmation and reset
 * links can be opened without a mail server. The log only names the file, because the message contains
 * one-time links that should not end up in log aggregation.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
class LoggingMailOutbox implements MailOutbox {

    private static final AtomicLong COUNTER = new AtomicLong();

    private final Path directory;

    LoggingMailOutbox(@Value("${app.mail.outbox-dir:mail-outbox}") String directory) {
        this.directory = Path.of(directory);
    }

    @Override
    public void send(Email email) {
        try {
            Files.createDirectories(directory);
            Path file = directory.resolve("%d-%03d.txt".formatted(Instant.now().toEpochMilli(), COUNTER.incrementAndGet() % 1000));
            Files.writeString(file, "To: " + email.to() + "\nSubject: " + email.subject() + "\n\n" + email.text() + "\n",
                    StandardCharsets.UTF_8);
            for (Email.Attachment attachment : email.attachments()) {
                Files.write(directory.resolve(file.getFileName() + "-" + attachment.filename().replaceAll("[^A-Za-z0-9._-]", "_")), attachment.data());
            }
            log.info("Email not sent (app.mail.enabled=false); saved to {}", file.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Could not save the development email", e);
        }
    }
}