package com.marzouki.shop.mail;

import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.CurrentActor;
import com.marzouki.shop.security.AppUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admins edit the wording of every email, preview it with sample data, send themselves a test, or go back to the original. */
@Controller
@RequiredArgsConstructor
public class EmailTemplateController {

    private final EmailTemplates templates;
    private final MailComposer composer;
    private final MailOutbox outbox;
    private final com.marzouki.shop.settings.SettingsService settings;

    @GetMapping("/admin/emails")
    public String list(Model model) {
        java.util.Map<EmailKind, Boolean> custom = new java.util.LinkedHashMap<>();
        for (EmailKind kind : EmailKind.values()) {
            custom.put(kind, templates.isCustom(kind));
        }
        model.addAttribute("kinds", custom);
        return "admin/emails";
    }

    @GetMapping("/admin/emails/{key}")
    public String edit(@PathVariable String key, Model model) {
        EmailKind kind = EmailKind.ofKey(key);
        EmailTemplateForm form = templates.form(kind);
        return view(kind, form, model);
    }

    @PostMapping("/admin/emails/{key}/preview")
    public String preview(@PathVariable String key, @Valid @ModelAttribute("form") EmailTemplateForm form, BindingResult result,
                          Model model) {
        EmailKind kind = EmailKind.ofKey(key);
        if (!result.hasErrors()) {
            try {
                templates.check(kind, form);
            } catch (ConflictException e) {
                result.rejectValue("body", "placeholder", e.getMessage());
            }
        }
        return view(kind, form, model);
    }

    @PostMapping("/admin/emails/{key}")
    public String save(@PathVariable String key, @Valid @ModelAttribute("form") EmailTemplateForm form, BindingResult result,
                       Model model, RedirectAttributes flash) {
        EmailKind kind = EmailKind.ofKey(key);
        if (!result.hasErrors()) {
            try {
                templates.save(kind, form, CurrentActor.id());
                flash.addFlashAttribute("success", "Saved. Future emails use this wording.");
                return "redirect:/admin/emails/" + kind.getKey();
            } catch (ConflictException e) {
                result.rejectValue("body", "placeholder", e.getMessage());
            }
        }
        return view(kind, form, model);
    }

    @PostMapping("/admin/emails/{key}/reset")
    public String reset(@PathVariable String key, RedirectAttributes flash) {
        EmailKind kind = EmailKind.ofKey(key);
        templates.reset(kind);
        flash.addFlashAttribute("success", "Back to the original wording");
        return "redirect:/admin/emails/" + kind.getKey();
    }

    @PostMapping("/admin/emails/{key}/test")
    public String test(@PathVariable String key, @AuthenticationPrincipal AppUserDetails me, RedirectAttributes flash) {
        EmailKind kind = EmailKind.ofKey(key);
        outbox.send(composer.sample(kind, me.getEmail()));
        flash.addFlashAttribute("info", "A test email was sent to " + me.getEmail() + " using the saved wording and sample data.");
        return "redirect:/admin/emails/" + kind.getKey();
    }

    private String view(EmailKind kind, EmailTemplateForm form, Model model) {
        model.addAttribute("kind", kind);
        model.addAttribute("form", form);
        model.addAttribute("custom", templates.isCustom(kind));
        model.addAttribute("accent", settings.get().getAccentColor());
        EmailTemplates.Wording shown = new EmailTemplates.Wording(nz(form.getSubject()), nz(form.getHeading()), nz(form.getBody()),
                nz(form.getButtonLabel()), true);
        model.addAttribute("preview", composer.preview(kind, shown));
        return "admin/email";
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}