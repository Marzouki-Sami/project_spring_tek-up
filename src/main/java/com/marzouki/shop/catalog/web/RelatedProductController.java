package com.marzouki.shop.catalog.web;

import com.marzouki.shop.catalog.ProductService;
import com.marzouki.shop.catalog.RelatedProducts;
import com.marzouki.shop.common.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Managers pick which products are suggested next to a product. */
@Controller
@RequiredArgsConstructor
public class RelatedProductController {

    private final RelatedProducts related;
    private final ProductService products;

    @GetMapping("/manage/products/{id}/related")
    public String page(@PathVariable Long id, Model model) {
        model.addAttribute("product", products.get(id));
        model.addAttribute("picked", related.picked(id));
        model.addAttribute("suggested", related.forProduct(id));
        return "product/related";
    }

    @PostMapping("/manage/products/{id}/related")
    public String add(@PathVariable Long id, @RequestParam String sku, RedirectAttributes flash) {
        try {
            related.add(id, sku);
            flash.addFlashAttribute("success", "Added to the related products");
        } catch (ConflictException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manage/products/" + id + "/related";
    }

    @PostMapping("/manage/products/{id}/related/{relatedId}/delete")
    public String remove(@PathVariable Long id, @PathVariable Long relatedId, RedirectAttributes flash) {
        related.remove(id, relatedId);
        flash.addFlashAttribute("success", "Removed from the related products");
        return "redirect:/manage/products/" + id + "/related";
    }
}