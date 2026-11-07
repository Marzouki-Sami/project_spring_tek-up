package com.marzouki.shop.catalog.web;

import com.marzouki.shop.catalog.OptionService;
import com.marzouki.shop.catalog.OptionType;
import com.marzouki.shop.catalog.dto.OptionTypeForm;
import com.marzouki.shop.catalog.dto.OptionValueForm;
import com.marzouki.shop.common.ConflictException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Option types such as Size and Color, and the values each one offers. */
@Controller
@RequestMapping("/manage/options")
@RequiredArgsConstructor
public class OptionController {

    private final OptionService options;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("form", new OptionTypeForm());
        return page(model);
    }

    @PostMapping
    public String createType(@Valid @ModelAttribute("form") OptionTypeForm form, BindingResult result, Model model,
                             RedirectAttributes flash) {
        if (!result.hasErrors()) {
            try {
                options.createType(form);
                flash.addFlashAttribute("success", "Option created");
                return "redirect:/manage/options";
            } catch (ConflictException e) {
                result.rejectValue("name", "taken", e.getMessage());
            }
        }
        return page(model);
    }

    @PostMapping("/{id}/delete")
    public String deleteType(@PathVariable Long id, RedirectAttributes flash) {
        return run(flash, "Option deleted", () -> options.deleteType(id));
    }

    @PostMapping("/{id}/values")
    public String addValue(@PathVariable Long id, @Valid @ModelAttribute OptionValueForm value, BindingResult result,
                           RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("error", result.getFieldErrors().get(0).getDefaultMessage());
            return "redirect:/manage/options";
        }
        return run(flash, "Value added", () -> options.addValue(id, value));
    }

    @PostMapping("/values/{valueId}/delete")
    public String deleteValue(@PathVariable Long valueId, RedirectAttributes flash) {
        return run(flash, "Value deleted", () -> options.deleteValue(valueId));
    }

    private String run(RedirectAttributes flash, String success, Runnable action) {
        try {
            action.run();
            flash.addFlashAttribute("success", success);
        } catch (ConflictException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manage/options";
    }

    private String page(Model model) {
        model.addAttribute("optionTypes", options.findAll());
        model.addAttribute("displays", OptionType.Display.values());
        return "option/options";
    }
}