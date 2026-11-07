package com.marzouki.shop.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
class SmtpMailOutbox implements MailOutbox {

    private final JavaMailSender sender;
    private final MailProperties properties;

    @Override
    public void send(Email email) {
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(properties.from());
            helper.setTo(email.to());
            helper.setSubject(email.subject());
            helper.setText(email.text(), email.html());
            for (Email.Attachment file : email.attachments()) {
                helper.addAttachment(file.filename(), new org.springframework.core.io.ByteArrayResource(file.data()), file.contentType());
            }
            sender.send(message);
        } catch (Exception e) {
            log.error("Could not send the email '{}'", email.subject(), e);
        }
    }
}