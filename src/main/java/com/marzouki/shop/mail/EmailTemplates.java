package com.marzouki.shop.mail;

import com.marzouki.shop.common.ConflictException;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The wording of each email: what an admin saved, or the built-in text when nothing was changed. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailTemplates {

    /** The text of an email before placeholders are filled in. */
    public record Wording(String subject, String heading, String body, String buttonLabel, boolean custom) {
    }

    private final EmailTemplateRepository repository;

    public Wording resolve(EmailKind kind) {
        return repository.findById(kind.getKey())
                .map(t -> new Wording(t.getSubject(), t.getHeading(), t.getBody(),
                        t.getButtonLabel() == null || t.getButtonLabel().isBlank() ? kind.getDefaultButton() : t.getButtonLabel(), true))
                .orElseGet(() -> defaults(kind));
    }

    public Wording defaults(EmailKind kind) {
        return new Wording(kind.getDefaultSubject(), kind.getDefaultHeading(), kind.getDefaultBody(), kind.getDefaultButton(), false);
    }

    public EmailTemplateForm form(EmailKind kind) {
        Wording wording = resolve(kind);
        EmailTemplateForm form = new EmailTemplateForm();
        form.setSubject(wording.subject());
        form.setHeading(wording.heading());
        form.setBody(wording.body());
        form.setButtonLabel(wording.buttonLabel());
        return form;
    }

    public boolean isCustom(EmailKind kind) {
        return repository.existsById(kind.getKey());
    }

    /** Refuses text that uses a placeholder the email does not offer, so a typo cannot reach a customer. */
    public void check(EmailKind kind, EmailTemplateForm form) {
        for (String text : new String[] {form.getSubject(), form.getHeading(), form.getBody(),
                form.getButtonLabel() == null ? "" : form.getButtonLabel()}) {
            Set<String> unknown = TemplateText.unknown(text, kind.getPlaceholders());
            if (!unknown.isEmpty()) {
                throw new ConflictException("{{" + unknown.iterator().next() + "}} is not available in this email. You can use: "
                        + String.join(", ", kind.getPlaceholders().stream().map(p -> "{{" + p + "}}").toList()));
            }
            if (TemplateText.hasBrokenPlaceholder(text)) {
                throw new ConflictException("A placeholder is not written correctly. Use the form {{name}}.");
            }
        }
    }

    @Transactional
    public void save(EmailKind kind, EmailTemplateForm form, Long actorId) {
        check(kind, form);
        EmailTemplate template = repository.findById(kind.getKey()).orElseGet(EmailTemplate::new);
        template.setKey(kind.getKey());
        template.setSubject(form.getSubject().trim());
        template.setHeading(form.getHeading().trim());
        template.setBody(form.getBody().strip());
        String button = form.getButtonLabel() == null ? "" : form.getButtonLabel().trim();
        template.setButtonLabel(button.isEmpty() ? null : button);
        template.setUpdatedAt(Instant.now());
        template.setUpdatedBy(actorId);
        repository.save(template);
    }

    @Transactional
    public void reset(EmailKind kind) {
        repository.deleteById(kind.getKey());
    }
}