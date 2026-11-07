package com.marzouki.shop.user.web;

import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.security.AppUserDetails;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** User administration, for admins only. */
@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService users;
    private final com.marzouki.shop.security.mfa.TwoFactorService twoFactor;

    @GetMapping
    public String list(@AuthenticationPrincipal AppUserDetails me, @org.springframework.web.bind.annotation.ModelAttribute("query") com.marzouki.shop.common.ListQuery query, Model model) {
        model.addAttribute("page", users.search(query));
        model.addAttribute("links", new com.marzouki.shop.common.PageLinks("/admin/users", query.params()));
        model.addAttribute("statuses", com.marzouki.shop.common.ListQuery.choices("", "Everyone", "CUSTOMER", "Customers", "MANAGER", "Managers", "ADMIN", "Admins",
                "NO2FA", "Staff without 2FA", "LOCKED", "Locked", "DISABLED", "Disabled"));
        model.addAttribute("sorts", com.marzouki.shop.common.ListQuery.choices("", "Name", "newest", "Newest first"));
        model.addAttribute("roles", Role.values());
        model.addAttribute("meId", me.getId());
        model.addAttribute("now", java.time.Instant.now());
        return "admin/users";
    }

    @PostMapping("/{id}/role")
    public String changeRole(@AuthenticationPrincipal AppUserDetails me, @PathVariable Long id,
                             @RequestParam Role role, RedirectAttributes flash) {
        return run(flash, "Role updated", () -> users.changeRole(me.getId(), id, role));
    }

    @PostMapping("/{id}/enabled")
    public String setEnabled(@AuthenticationPrincipal AppUserDetails me, @PathVariable Long id,
                             @RequestParam boolean enabled, RedirectAttributes flash) {
        return run(flash, enabled ? "Account enabled" : "Account disabled",
                () -> users.setEnabled(me.getId(), id, enabled));
    }

    @PostMapping("/{id}/reset-2fa")
    public String reset2fa(@PathVariable Long id, RedirectAttributes flash) {
        return run(flash, "Two-factor authentication was reset. The user sets it up again at the next sign-in.", () -> twoFactor.reset(id));
    }

    @PostMapping("/{id}/unlock")
    public String unlock(@PathVariable Long id, RedirectAttributes flash) {
        return run(flash, "Account unlocked", () -> users.unlock(id));
    }

    private String run(RedirectAttributes flash, String success, Runnable action) {
        try {
            action.run();
            flash.addFlashAttribute("success", success);
        } catch (ConflictException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}