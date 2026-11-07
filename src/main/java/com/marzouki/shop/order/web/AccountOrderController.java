package com.marzouki.shop.order.web;

import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.order.OrderService;
import com.marzouki.shop.security.AppUserDetails;
import com.marzouki.shop.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** A customer's own order history. */
@Controller
@RequestMapping("/account/orders")
@RequiredArgsConstructor
public class AccountOrderController {

    private final OrderService orders;

    @GetMapping
    public String list(@AuthenticationPrincipal AppUserDetails me,
                       @org.springframework.web.bind.annotation.ModelAttribute("query") com.marzouki.shop.common.ListQuery query, Model model) {
        model.addAttribute("page", orders.search(query, me.getId()));
        model.addAttribute("links", new com.marzouki.shop.common.PageLinks("/account/orders", query.params()));
        java.util.List<String> pairs = new java.util.ArrayList<>(java.util.List.of("", "All orders"));
        for (OrderStatus s : OrderStatus.values()) {
            pairs.add(s.name());
            pairs.add(s.getLabel());
        }
        model.addAttribute("statuses", com.marzouki.shop.common.ListQuery.choices(pairs.toArray(String[]::new)));
        model.addAttribute("sorts", com.marzouki.shop.common.ListQuery.choices("", "Newest first", "oldest", "Oldest first", "total-desc", "Highest total", "total-asc", "Lowest total"));
        return "order/my-orders";
    }

    @GetMapping("/{id}")
    public String show(@AuthenticationPrincipal AppUserDetails me, @PathVariable Long id, Model model) {
        model.addAttribute("order", orders.getOwned(id, me.getId()));
        model.addAttribute("manager", false);
        return "order/detail";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@AuthenticationPrincipal AppUserDetails me, @PathVariable Long id,
                         RedirectAttributes flash) {
        try {
            orders.cancelOwn(id, me.getId());
            flash.addFlashAttribute("success", "Your order was cancelled");
        } catch (ConflictException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/account/orders/" + id;
    }
}