package com.marzouki.shop.security.mfa;

import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.security.AppUserDetails;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Setting up and using two-factor authentication on the website. */
@Controller
@RequiredArgsConstructor
class TwoFactorController {

    private static final String PENDING_SECRET = "mfa.pendingSecret";

    private final TwoFactorService twoFactor;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final TrustCookie trustCookie;

    @GetMapping("/2fa/setup")
    String setupPage(@AuthenticationPrincipal AppUserDetails me, HttpSession session, Model model) {
        User user = user(me);
        if (user.isTotpEnabled()) {
            return "redirect:/account/security";
        }
        String secret = (String) session.getAttribute(PENDING_SECRET);
        if (secret == null) {
            secret = twoFactor.newSecret();
            session.setAttribute(PENDING_SECRET, secret);
        }
        return setupView(user, secret, model);
    }

    @PostMapping("/2fa/setup")
    String setup(@AuthenticationPrincipal AppUserDetails me, @RequestParam String code, HttpSession session,
                 Model model, RedirectAttributes flash) {
        User user = user(me);
        String secret = (String) session.getAttribute(PENDING_SECRET);
        if (secret == null || user.isTotpEnabled()) {
            return "redirect:/2fa/setup";
        }
        try {
            List<String> codes = twoFactor.enable(user.getId(), secret, code);
            session.removeAttribute(PENDING_SECRET);
            MfaLoginSuccessHandler.markVerified(session, user.getId());
            flash.addFlashAttribute("recoveryCodes", codes);
            return "redirect:/2fa/recovery-codes";
        } catch (ConflictException e) {
            model.addAttribute("error", e.getMessage());
            return setupView(user, secret, model);
        }
    }

    @GetMapping("/2fa/recovery-codes")
    String recoveryCodes(Model model) {
        if (!model.containsAttribute("recoveryCodes")) {
            return "redirect:/account/security";
        }
        return "auth/recovery-codes";
    }

    @GetMapping("/2fa/verify")
    String verifyPage(@AuthenticationPrincipal AppUserDetails me, HttpSession session, Model model) {
        User user = user(me);
        if (!user.isTotpEnabled()) {
            return "redirect:/2fa/setup";
        }
        if (MfaLoginSuccessHandler.isVerified(session, me.getId())) {
            return "redirect:/";
        }
        model.addAttribute("trustDays", twoFactor.trustDays());
        return "auth/two-factor-verify";
    }

    @PostMapping("/2fa/verify")
    String verify(@AuthenticationPrincipal AppUserDetails me, @RequestParam String code,
                  @RequestParam(defaultValue = "false") boolean trust, HttpServletRequest request,
                  HttpServletResponse response, Model model) {
        try {
            if (twoFactor.verify(me.getId(), code)) {
                MfaLoginSuccessHandler.markVerified(request.getSession(), me.getId());
                if (trust) {
                    trustCookie.write(response, twoFactor.trustDevice(me.getId(), request.getHeader("User-Agent")),
                            twoFactor.trustDays());
                }
                return "redirect:/";
            }
            model.addAttribute("error", "That code is not right, or it was already used.");
        } catch (ConflictException e) {
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("trustDays", twoFactor.trustDays());
        return "auth/two-factor-verify";
    }

    @GetMapping("/account/security")
    String security(@AuthenticationPrincipal AppUserDetails me, Model model) {
        User user = user(me);
        model.addAttribute("enabled", user.isTotpEnabled());
        model.addAttribute("required", twoFactor.isRequiredFor(user.getRole()));
        model.addAttribute("codesLeft", twoFactor.recoveryCodesLeft(user.getId()));
        model.addAttribute("devices", twoFactor.trustedDevices(user.getId()));
        model.addAttribute("trustDays", twoFactor.trustDays());
        return "account/security";
    }

    @PostMapping("/account/security/recovery-codes")
    String newCodes(@AuthenticationPrincipal AppUserDetails me, @RequestParam String code, RedirectAttributes flash) {
        try {
            flash.addFlashAttribute("recoveryCodes", twoFactor.regenerateRecoveryCodes(me.getId(), code));
            return "redirect:/2fa/recovery-codes";
        } catch (ConflictException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/account/security";
        }
    }

    @PostMapping("/account/security/devices/forget")
    String forget(@AuthenticationPrincipal AppUserDetails me, HttpServletResponse response, RedirectAttributes flash) {
        twoFactor.forgetDevices(me.getId());
        trustCookie.clear(response);
        flash.addFlashAttribute("success", "All trusted devices were forgotten");
        return "redirect:/account/security";
    }

    @PostMapping("/account/security/disable")
    String disable(@AuthenticationPrincipal AppUserDetails me, @RequestParam String password, HttpSession session,
                   HttpServletResponse response, RedirectAttributes flash) {
        User user = user(me);
        if (!encoder.matches(password, user.getPasswordHash())) {
            flash.addFlashAttribute("error", "The password is not correct");
            return "redirect:/account/security";
        }
        try {
            twoFactor.disable(user.getId());
            trustCookie.clear(response);
            session.removeAttribute(MfaLoginSuccessHandler.VERIFIED);
            flash.addFlashAttribute("success", "Two-factor authentication is off");
        } catch (ConflictException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/account/security";
    }

    private String setupView(User user, String secret, Model model) {
        model.addAttribute("qr", twoFactor.qrFor(user, secret));
        model.addAttribute("secret", String.join(" ", secret.split("(?<=\\G.{4})")));
        model.addAttribute("required", twoFactor.isRequiredFor(user.getRole()));
        return "auth/two-factor-setup";
    }

    private User user(AppUserDetails me) {
        return users.findById(me.getId()).orElseThrow();
    }
}