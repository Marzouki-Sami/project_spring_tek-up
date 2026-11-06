package com.marzouki.shop.order.web;

import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.order.OrderService;
import com.marzouki.shop.order.dto.CouponForm;
import jakarta.validation.Valid;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manage/coupons")
@RequiredArgsConstructor
public class ManageCouponController {

    private final OrderService orders;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("form", new CouponForm());
        return page(model);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CouponForm form, BindingResult result, Model model,
                         RedirectAttributes flash) {
        if (!result.hasErrors()) {
            try {
                orders.createCoupon(form.getCode(), form.getPercentOff(),
                        form.getExpiresOn() == null ? null
                                : form.getExpiresOn().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                        form.getMaxUses());
                flash.addFlashAttribute("success", "Coupon created");
                return "redirect:/manage/coupons";
            } catch (ConflictException e) {
                result.rejectValue("code", "taken", e.getMessage());
            }
        }
        return page(model);
    }

    @PostMapping("/{id}/active")
    public String setActive(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes flash) {
        orders.setCouponActive(id, active);
        flash.addFlashAttribute("success", active ? "Coupon enabled" : "Coupon disabled");
        return "redirect:/manage/coupons";
    }

    private String page(Model model) {
        model.addAttribute("coupons", orders.coupons());
        model.addAttribute("now", java.time.Instant.now());
        return "order/coupons";
    }
}