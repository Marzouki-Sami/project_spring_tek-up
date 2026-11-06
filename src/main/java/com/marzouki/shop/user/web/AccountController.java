package com.marzouki.shop.user.web;

import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.security.AppUserDetails;
import com.marzouki.shop.security.SessionLogin;
import com.marzouki.shop.user.UserService;
import com.marzouki.shop.user.dto.PasswordChangeForm;
import com.marzouki.shop.user.dto.ProfileForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** A signed-in user's own account. */
@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService users;
    private final SessionLogin sessionLogin;

    @GetMapping
    public String show(@AuthenticationPrincipal AppUserDetails me, Model model) {
        model.addAttribute("profile", users.profileFor(me.getId()));
        model.addAttribute("password", new PasswordChangeForm());
        model.addAttribute("verified", users.get(me.getId()).isEmailVerified());
        model.addAttribute("email", me.getEmail());
        model.addAttribute("role", me.getRole());
        return "account/account";
    }

    @PostMapping("/profile")
    public String saveProfile(@AuthenticationPrincipal AppUserDetails me,
                              @Valid @ModelAttribute("profile") ProfileForm form, BindingResult result,
                              Model model, HttpServletRequest request, HttpServletResponse response,
                              RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("password", new PasswordChangeForm());
            model.addAttribute("verified", users.get(me.getId()).isEmailVerified());
            model.addAttribute("email", me.getEmail());
            model.addAttribute("role", me.getRole());
            return "account/account";
        }
        sessionLogin.start(users.updateProfile(me.getId(), form), request, response);
        flash.addFlashAttribute("success", "Your details were saved");
        return "redirect:/account";
    }

    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal AppUserDetails me,
                                 @Valid @ModelAttribute("password") PasswordChangeForm form, BindingResult result,
                                 Model model, RedirectAttributes flash) {
        if (!result.hasFieldErrors("newPassword") && !result.hasFieldErrors("confirmPassword")
                && !form.getNewPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "The passwords do not match");
        }
        if (!result.hasErrors()) {
            try {
                users.changePassword(me.getId(), form);
                flash.addFlashAttribute("success", "Your password was changed");
                return "redirect:/account";
            } catch (ConflictException e) {
                result.rejectValue("currentPassword", "wrong", e.getMessage());
            }
        }
        model.addAttribute("profile", users.profileFor(me.getId()));
        model.addAttribute("verified", users.get(me.getId()).isEmailVerified());
        model.addAttribute("email", me.getEmail());
        model.addAttribute("role", me.getRole());
        return "account/account";
    }
}