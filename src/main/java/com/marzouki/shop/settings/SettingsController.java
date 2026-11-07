package com.marzouki.shop.settings;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Branding, company details and VAT, edited by admins. */
@Controller
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settings;

    @GetMapping("/admin/settings")
    public String show(org.springframework.ui.Model model) {
        model.addAttribute("form", settings.form());
        return "admin/settings";
    }

    @PostMapping("/admin/settings")
    public String save(@Valid @ModelAttribute("form") SettingsForm form, BindingResult result, RedirectAttributes flash) {
        if (result.hasErrors()) {
            return "admin/settings";
        }
        settings.save(form);
        flash.addFlashAttribute("success", "Settings saved");
        return "redirect:/admin/settings";
    }
}