package com.marzouki.shop.catalog.web;

import com.marzouki.shop.catalog.CategoryService;
import com.marzouki.shop.catalog.SubcategoryService;
import com.marzouki.shop.catalog.dto.SubcategoryForm;
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
@RequestMapping("/subcategories")
@RequiredArgsConstructor
public class SubcategoryController {

    private final SubcategoryService subcategories;
    private final CategoryService categories;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("subcategories", subcategories.findAll());
        return "subcategory/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        return show(model, new SubcategoryForm(), "/subcategories", "New subcategory");
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") SubcategoryForm form, BindingResult result,
                         Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            return show(model, form, "/subcategories", "New subcategory");
        }
        subcategories.create(form);
        flash.addFlashAttribute("success", "Subcategory created");
        return "redirect:/subcategories";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return show(model, subcategories.formFor(id), "/subcategories/" + id, "Edit subcategory");
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") SubcategoryForm form,
                         BindingResult result, Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            return show(model, form, "/subcategories/" + id, "Edit subcategory");
        }
        subcategories.update(id, form);
        flash.addFlashAttribute("success", "Subcategory saved");
        return "redirect:/subcategories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        subcategories.delete(id);
        flash.addFlashAttribute("success", "Subcategory deleted");
        return "redirect:/subcategories";
    }

    private String show(Model model, SubcategoryForm form, String action, String heading) {
        model.addAttribute("form", form);
        model.addAttribute("action", action);
        model.addAttribute("heading", heading);
        model.addAttribute("categories", categories.findAll());
        return "subcategory/form";
    }
}