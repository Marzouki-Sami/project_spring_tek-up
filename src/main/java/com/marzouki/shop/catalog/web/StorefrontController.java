package com.marzouki.shop.catalog.web;

import com.marzouki.shop.catalog.CategoryRepository;
import com.marzouki.shop.catalog.ProductRepository;
import com.marzouki.shop.common.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The public shop window: anyone can browse, no account needed. */
@Controller
@RequiredArgsConstructor
public class StorefrontController {

    static final int PAGE_SIZE = 12;

    private final ProductRepository products;
    private final CategoryRepository categories;

    @GetMapping("/")
    @Transactional(readOnly = true)
    public String home(Model model) {
        model.addAttribute("latest", products.findTop8ByActiveTrueOrderByCreatedAtDescIdDesc());
        model.addAttribute("categories", categories.findAllByOrderByTitleAsc());
        return "storefront/home";
    }

    @GetMapping("/products")
    @Transactional(readOnly = true)
    public String browse(@RequestParam(defaultValue = "") String q,
                         @RequestParam(required = false) Long category,
                         @RequestParam(defaultValue = "name") String sort,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        Sort order = switch (sort) {
            case "price-asc" -> Sort.by("price").ascending();
            case "price-desc" -> Sort.by("price").descending();
            case "newest" -> Sort.by("createdAt").descending().and(Sort.by("id").descending());
            default -> Sort.by("name").ascending();
        };
        String text = q.trim();
        Page<?> result = products.search(text, category, PageRequest.of(Math.max(page, 0), PAGE_SIZE, order));
        model.addAttribute("page", result);
        model.addAttribute("q", text);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("sort", sort);
        model.addAttribute("categories", categories.findAllByOrderByTitleAsc());
        return "storefront/products";
    }

    @GetMapping("/products/{id}")
    @Transactional(readOnly = true)
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("product", products.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Product", id)));
        return "storefront/product";
    }
}