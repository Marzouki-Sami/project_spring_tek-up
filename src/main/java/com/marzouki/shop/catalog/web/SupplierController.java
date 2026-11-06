package com.marzouki.shop.catalog.web;

import com.marzouki.shop.catalog.SupplierService;
import com.marzouki.shop.catalog.dto.SupplierForm;
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

@Controller
@RequestMapping("/manage/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService suppliers;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("suppliers", suppliers.findAll());
        return "supplier/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        return show(model, new SupplierForm(), "/manage/suppliers", "New supplier");
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") SupplierForm form, BindingResult result,
                         Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            return show(model, form, "/manage/suppliers", "New supplier");
        }
        suppliers.create(form);
        flash.addFlashAttribute("success", "Supplier created");
        return "redirect:/manage/suppliers";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return show(model, suppliers.formFor(id), "/manage/suppliers/" + id, "Edit supplier");
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") SupplierForm form,
                         BindingResult result, Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            return show(model, form, "/manage/suppliers/" + id, "Edit supplier");
        }
        suppliers.update(id, form);
        flash.addFlashAttribute("success", "Supplier saved");
        return "redirect:/manage/suppliers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        suppliers.delete(id);
        flash.addFlashAttribute("success", "Supplier deleted");
        return "redirect:/manage/suppliers";
    }

    private String show(Model model, SupplierForm form, String action, String heading) {
        model.addAttribute("form", form);
        model.addAttribute("action", action);
        model.addAttribute("heading", heading);
        return "supplier/form";
    }
}