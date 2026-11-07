package com.marzouki.shop.catalog.web;

import com.marzouki.shop.catalog.Product;
import com.marzouki.shop.catalog.ProductVariant;
import com.marzouki.shop.catalog.VariantService;
import com.marzouki.shop.catalog.dto.StockAdjustmentForm;
import com.marzouki.shop.catalog.dto.VariantForm;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.order.StockMovement;
import com.marzouki.shop.order.StockService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Variants of a product and their stock. */
@Controller
@RequiredArgsConstructor
public class VariantController {

    private final VariantService variants;
    private final StockService stock;

    @GetMapping("/manage/products/{productId}/variants")
    public String list(@PathVariable Long productId, Model model) {
        Product product = variants.productWithVariants(productId);
        model.addAttribute("product", product);
        model.addAttribute("allowed", variants.allowedOptions(product));
        model.addAttribute("used", variants.usedValues(product));
        return "variant/variants";
    }

    @PostMapping("/manage/products/{productId}/variants/generate")
    public String generate(@PathVariable Long productId,
                           @RequestParam(name = "values", required = false) java.util.List<String> values,
                           RedirectAttributes flash) {
        Map<Long, Set<Long>> chosen = new HashMap<>();
        if (values != null) {
            for (String pair : values) {
                String[] parts = pair.split(":");
                if (parts.length == 2) {
                    chosen.computeIfAbsent(parseId(parts[0]), k -> new LinkedHashSet<>()).add(parseId(parts[1]));
                }
            }
        }
        try {
            VariantService.Generated result = variants.generate(productId, chosen);
            if (result.created() == 0) {
                flash.addFlashAttribute("warning", result.alreadyExisted() == 1
                        ? "That variant already exists, so nothing was created"
                        : "All " + result.alreadyExisted() + " of those variants already exist, so nothing was created");
            } else if (result.alreadyExisted() > 0) {
                flash.addFlashAttribute("success", result.created() + (result.created() == 1 ? " variant created" : " variants created")
                        + ". " + result.alreadyExisted() + " already existed and were left as they are.");
            } else {
                flash.addFlashAttribute("success", result.created() + (result.created() == 1 ? " variant created" : " variants created"));
            }
        } catch (ConflictException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manage/products/" + productId + "/variants";
    }

    @PostMapping("/manage/products/{productId}/variants/{variantId}")
    public String update(@PathVariable Long productId, @PathVariable Long variantId,
                         @Valid @ModelAttribute VariantForm form, BindingResult result, RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("error", result.getFieldErrors().get(0).getDefaultMessage());
        } else {
            try {
                variants.update(productId, variantId, form);
                flash.addFlashAttribute("success", "Variant saved");
            } catch (ConflictException e) {
                flash.addFlashAttribute("error", e.getMessage());
            }
        }
        return "redirect:/manage/products/" + productId + "/variants";
    }

    @PostMapping("/manage/products/{productId}/variants/{variantId}/delete")
    public String delete(@PathVariable Long productId, @PathVariable Long variantId, RedirectAttributes flash) {
        try {
            variants.delete(productId, variantId);
            flash.addFlashAttribute("success", "Variant deleted");
        } catch (ConflictException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manage/products/" + productId + "/variants";
    }

    @GetMapping("/manage/variants/{variantId}/stock")
    public String stockPage(@PathVariable Long variantId, @RequestParam(defaultValue = "0") int page, Model model) {
        return stockView(variantId, page, new StockAdjustmentForm(), model);
    }

    @PostMapping("/manage/variants/{variantId}/stock")
    public String adjust(@PathVariable Long variantId, @Valid @ModelAttribute("form") StockAdjustmentForm form,
                         BindingResult result, Model model, RedirectAttributes flash) {
        if (!result.hasErrors()) {
            try {
                stock.adjust(variantId, form.getDelta(), form.getReason(), form.getNote());
                int now = variants.getForStock(variantId).getStock();
                if (now == 0) {
                    flash.addFlashAttribute("warning", "Stock updated. This item is now sold out.");
                } else if (now <= com.marzouki.shop.order.OrderService.LOW_STOCK_LEVEL) {
                    flash.addFlashAttribute("warning", "Stock updated. Only " + now + " left.");
                } else {
                    flash.addFlashAttribute("success", "Stock updated. " + now + " in stock.");
                }
                return "redirect:/manage/variants/" + variantId + "/stock";
            } catch (ConflictException e) {
                result.rejectValue("delta", "invalid", e.getMessage());
            }
        }
        return stockView(variantId, 0, form, model);
    }

    private String stockView(Long variantId, int page, StockAdjustmentForm form, Model model) {
        ProductVariant variant = variants.getForStock(variantId);
        model.addAttribute("variant", variant);
        model.addAttribute("form", form);
        model.addAttribute("reasons", StockMovement.Reason.manual());
        model.addAttribute("history", stock.history(variantId, page));
        return "variant/stock";
    }

    private static Long parseId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new ConflictException("Invalid selection");
        }
    }
}