package com.marzouki.shop.catalog.web;

import com.marzouki.shop.catalog.OptionService;
import com.marzouki.shop.catalog.ProductTypeService;
import com.marzouki.shop.catalog.dto.ProductTypeForm;
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

/** Product types: which options (Size, Color, ...) the products of a family come in. */
@Controller
@RequestMapping("/manage/product-types")
@RequiredArgsConstructor
public class ProductTypeController {

    private final ProductTypeService types;
    private final OptionService options;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("types", types.findAll());
        return "product-type/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        return form(model, new ProductTypeForm(), "/manage/product-types", "New product type");
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ProductTypeForm form, BindingResult result, Model model,
                         RedirectAttributes flash) {
        if (!result.hasErrors()) {
            try {
                types.create(form);
                flash.addFlashAttribute("success", "Product type created");
                return "redirect:/manage/product-types";
            } catch (ConflictException e) {
                result.rejectValue("name", "taken", e.getMessage());
            }
        }
        return form(model, form, "/manage/product-types", "New product type");
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return form(model, types.formFor(id), "/manage/product-types/" + id, "Edit product type");
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") ProductTypeForm form,
                         BindingResult result, Model model, RedirectAttributes flash) {
        if (!result.hasErrors()) {
            try {
                types.update(id, form);
                flash.addFlashAttribute("success", "Product type saved");
                return "redirect:/manage/product-types";
            } catch (ConflictException e) {
                result.reject("conflict", e.getMessage());
            }
        }
        return form(model, form, "/manage/product-types/" + id, "Edit product type");
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            types.delete(id);
            flash.addFlashAttribute("success", "Product type deleted");
        } catch (ConflictException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manage/product-types";
    }

    private String form(Model model, ProductTypeForm form, String action, String heading) {
        model.addAttribute("form", form);
        model.addAttribute("action", action);
        model.addAttribute("heading", heading);
        model.addAttribute("optionTypes", options.findAll());
        return "product-type/form";
    }
}