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
    public String list(@org.springframework.web.bind.annotation.ModelAttribute("query") com.marzouki.shop.common.ListQuery query, Model model) {
        model.addAttribute("form", new CouponForm());
        return page(model, query);
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
        return page(model, new com.marzouki.shop.common.ListQuery());
    }

    @PostMapping("/{id}/active")
    public String setActive(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes flash) {
        orders.setCouponActive(id, active);
        flash.addFlashAttribute("success", active ? "Coupon enabled" : "Coupon disabled");
        return "redirect:/manage/coupons";
    }

    private String page(Model model, com.marzouki.shop.common.ListQuery query) {
        model.addAttribute("query", query);
        model.addAttribute("page", orders.couponPage(query));
        model.addAttribute("exportPath", "/manage/coupons/export.csv");
        model.addAttribute("links", new com.marzouki.shop.common.PageLinks("/manage/coupons", query.params()));
        model.addAttribute("statuses", com.marzouki.shop.common.ListQuery.choices("", "All coupons", "USABLE", "Usable", "EXPIRED", "Expired", "DISABLED", "Disabled"));
        model.addAttribute("sorts", com.marzouki.shop.common.ListQuery.choices("", "Newest first", "code", "Code", "used", "Most used"));
        model.addAttribute("now", java.time.Instant.now());
        return "order/coupons";
    }
}