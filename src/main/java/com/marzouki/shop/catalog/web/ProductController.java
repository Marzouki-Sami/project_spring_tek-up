package com.marzouki.shop.catalog.web;

import com.marzouki.shop.catalog.ProductService;
import com.marzouki.shop.catalog.SubcategoryService;
import com.marzouki.shop.catalog.SupplierService;
import com.marzouki.shop.catalog.dto.ProductForm;
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
@RequestMapping("/manage/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService products;
    private final SubcategoryService subcategories;
    private final SupplierService suppliers;
    private final com.marzouki.shop.catalog.ProductTypeService productTypes;

    @GetMapping
    public String list(@org.springframework.web.bind.annotation.ModelAttribute("query") com.marzouki.shop.common.ListQuery query, Model model) {
        model.addAttribute("page", products.search(query));
        model.addAttribute("links", new com.marzouki.shop.common.PageLinks("/manage/products", query.params()));
        model.addAttribute("statuses", com.marzouki.shop.common.ListQuery.choices("", "All products", "ACTIVE", "Active",
                "HIDDEN", "Hidden", "LOW", "Low stock", "OUT", "Sold out"));
        model.addAttribute("sorts", com.marzouki.shop.common.ListQuery.choices("", "Name", "sku", "SKU", "newest", "Newest",
                "price-asc", "Price: low to high", "price-desc", "Price: high to low"));
        return "product/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        return show(model, new ProductForm(), "/manage/products", "New product");
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ProductForm form, BindingResult result,
                         Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            return show(model, form, "/manage/products", "New product");
        }
        com.marzouki.shop.catalog.Product created = products.create(form);
        flash.addFlashAttribute("success", "Product created");
        return created.isOptioned() ? "redirect:/manage/products/" + created.getId() + "/variants"
                : "redirect:/manage/products";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("existing", products.get(id));
        return show(model, products.formFor(id), "/manage/products/" + id, "Edit product");
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") ProductForm form,
                         BindingResult result, Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            return show(model, form, "/manage/products/" + id, "Edit product");
        }
        try {
            products.update(id, form);
        } catch (com.marzouki.shop.common.ConflictException e) {
            result.reject("conflict", e.getMessage());
            return show(model, form, "/manage/products/" + id, "Edit product");
        }
        flash.addFlashAttribute("success", "Product saved");
        return "redirect:/manage/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        products.delete(id);
        flash.addFlashAttribute("success", "Product deleted");
        return "redirect:/manage/products";
    }

    private String show(Model model, ProductForm form, String action, String heading) {
        model.addAttribute("form", form);
        model.addAttribute("action", action);
        model.addAttribute("heading", heading);
        model.addAttribute("subcategories", subcategories.findAll());
        model.addAttribute("suppliers", suppliers.findAll());
        model.addAttribute("productTypes", productTypes.findAll());
        model.addAttribute("creating", action.equals("/manage/products"));
        return "product/form";
    }
}