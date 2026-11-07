package com.marzouki.shop.catalog.web;

import com.marzouki.shop.catalog.CategoryService;
import com.marzouki.shop.catalog.dto.CategoryForm;
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
@RequestMapping("/manage/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categories;

    @GetMapping
    public String list(@org.springframework.web.bind.annotation.ModelAttribute("query") com.marzouki.shop.common.ListQuery query, Model model) {
        model.addAttribute("page", categories.search(query));
        model.addAttribute("links", new com.marzouki.shop.common.PageLinks("/manage/categories", query.params()));
        return "category/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new CategoryForm());
        model.addAttribute("action", "/manage/categories");
        model.addAttribute("heading", "New category");
        return "category/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CategoryForm form, BindingResult result,
                         Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("action", "/manage/categories");
            model.addAttribute("heading", "New category");
            return "category/form";
        }
        categories.create(form);
        flash.addFlashAttribute("success", "Category created");
        return "redirect:/manage/categories";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", categories.formFor(id));
        model.addAttribute("action", "/manage/categories/" + id);
        model.addAttribute("heading", "Edit category");
        return "category/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") CategoryForm form,
                         BindingResult result, Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("action", "/manage/categories/" + id);
            model.addAttribute("heading", "Edit category");
            return "category/form";
        }
        categories.update(id, form);
        flash.addFlashAttribute("success", "Category saved");
        return "redirect:/manage/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        categories.delete(id);
        flash.addFlashAttribute("success", "Category deleted");
        return "redirect:/manage/categories";
    }
}