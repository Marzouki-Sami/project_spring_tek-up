package com.marzouki.shop.address.web;

import com.marzouki.shop.address.AddressService;
import com.marzouki.shop.address.dto.AddressForm;
import com.marzouki.shop.common.ConflictException;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** A customer's address book. */
@Controller
@RequestMapping("/account/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addresses;

    @GetMapping
    public String list(@AuthenticationPrincipal AppUserDetails me, Model model) {
        model.addAttribute("addresses", addresses.forUser(me.getId()));
        model.addAttribute("max", AddressService.MAX_ADDRESSES);
        return "address/list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal AppUserDetails me, Model model) {
        AddressForm form = new AddressForm();
        form.setFullName(me.getFullName());
        form.setLabel("Home");
        return show(model, form, "/account/addresses", "New address");
    }

    @PostMapping
    public String create(@AuthenticationPrincipal AppUserDetails me, @Valid @ModelAttribute("form") AddressForm form,
                         BindingResult result, Model model, RedirectAttributes flash) {
        if (!result.hasErrors()) {
            try {
                addresses.create(me.getId(), form);
                flash.addFlashAttribute("success", "Address saved");
                return "redirect:/account/addresses";
            } catch (ConflictException e) {
                result.reject("conflict", e.getMessage());
            }
        }
        return show(model, form, "/account/addresses", "New address");
    }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal AppUserDetails me, @PathVariable Long id, Model model) {
        return show(model, addresses.formFor(id, me.getId()), "/account/addresses/" + id, "Edit address");
    }

    @PostMapping("/{id}")
    public String update(@AuthenticationPrincipal AppUserDetails me, @PathVariable Long id,
                         @Valid @ModelAttribute("form") AddressForm form, BindingResult result, Model model,
                         RedirectAttributes flash) {
        if (result.hasErrors()) {
            return show(model, form, "/account/addresses/" + id, "Edit address");
        }
        addresses.update(id, me.getId(), form);
        flash.addFlashAttribute("success", "Address saved");
        return "redirect:/account/addresses";
    }

    @PostMapping("/{id}/default")
    public String makeDefault(@AuthenticationPrincipal AppUserDetails me, @PathVariable Long id,
                              RedirectAttributes flash) {
        addresses.makeDefault(id, me.getId());
        flash.addFlashAttribute("success", "Default address changed");
        return "redirect:/account/addresses";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal AppUserDetails me, @PathVariable Long id, RedirectAttributes flash) {
        addresses.delete(id, me.getId());
        flash.addFlashAttribute("info", "Address deleted");
        return "redirect:/account/addresses";
    }

    private String show(Model model, AddressForm form, String action, String heading) {
        model.addAttribute("form", form);
        model.addAttribute("action", action);
        model.addAttribute("heading", heading);
        return "address/form";
    }
}