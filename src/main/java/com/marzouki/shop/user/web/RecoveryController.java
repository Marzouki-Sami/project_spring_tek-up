package com.marzouki.shop.user.web;

import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.security.AppUserDetails;
import com.marzouki.shop.user.UserService;
import com.marzouki.shop.user.dto.ForgotPasswordForm;
import com.marzouki.shop.user.dto.ResetPasswordForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Email verification and password recovery. */
@Controller
@RequiredArgsConstructor
public class RecoveryController {

    private final UserService users;

    @GetMapping("/verify-email")
    public String verify(@RequestParam(defaultValue = "") String token, RedirectAttributes flash) {
        try {
            users.verifyEmail(token);
            flash.addFlashAttribute("success", "Your email address is confirmed. You can sign in now.");
        } catch (ConflictException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/login";
    }

    @PostMapping("/account/verify/resend")
    public String resend(@AuthenticationPrincipal AppUserDetails me, RedirectAttributes flash) {
        users.resendVerification(me.getId());
        flash.addFlashAttribute("success", "We sent you a new confirmation email");
        return "redirect:/account";
    }

    @GetMapping("/forgot-password")
    public String forgotForm(Model model) {
        model.addAttribute("form", new ForgotPasswordForm());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgot(@Valid @ModelAttribute("form") ForgotPasswordForm form, BindingResult result,
                         RedirectAttributes flash) {
        if (result.hasErrors()) {
            return "auth/forgot-password";
        }
        users.requestPasswordReset(form.getEmail());
        flash.addFlashAttribute("info",
                "If an account exists for that address, we sent a link to reset the password.");
        return "redirect:/login";
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam(defaultValue = "") String token, Model model) {
        ResetPasswordForm form = new ResetPasswordForm();
        form.setToken(token);
        model.addAttribute("form", form);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String reset(@Valid @ModelAttribute("form") ResetPasswordForm form, BindingResult result,
                        RedirectAttributes flash) {
        if (!result.hasFieldErrors("newPassword") && !result.hasFieldErrors("confirmPassword")
                && !form.getNewPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "The passwords do not match");
        }
        if (!result.hasErrors()) {
            try {
                users.resetPassword(form.getToken(), form.getNewPassword());
                flash.addFlashAttribute("success", "Your password was changed. Sign in with the new one.");
                return "redirect:/login";
            } catch (ConflictException e) {
                result.reject("token", e.getMessage());
            }
        }
        return "auth/reset-password";
    }
}