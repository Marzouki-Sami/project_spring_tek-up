package com.marzouki.shop.order.web;

import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.order.OrderService;
import com.marzouki.shop.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manage/orders")
@RequiredArgsConstructor
public class ManageOrderController {

    private final OrderService orders;

    @GetMapping
    public String list(@RequestParam(required = false) OrderStatus status, Model model) {
        model.addAttribute("orders", orders.all(status));
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("status", status);
        return "order/manage-orders";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        model.addAttribute("order", orders.get(id));
        model.addAttribute("manager", true);
        return "order/detail";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam OrderStatus status, RedirectAttributes flash) {
        try {
            orders.changeStatus(id, status);
            flash.addFlashAttribute("success", "Order marked as " + status.getLabel().toLowerCase());
        } catch (ConflictException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manage/orders/" + id;
    }
}