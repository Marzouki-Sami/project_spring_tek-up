package com.marzouki.shop.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboard;

    @GetMapping("/manage")
    public String show(Model model) {
        model.addAttribute("overview", dashboard.overview());
        return "dashboard/dashboard";
    }
}