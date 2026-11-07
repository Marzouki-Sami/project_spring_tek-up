package com.marzouki.shop.user.web;

import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.security.SessionLogin;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserService;
import com.marzouki.shop.user.dto.RegistrationForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService users;
    private final SessionLogin sessionLogin;
    private final com.marzouki.shop.cart.CartStore cartStore;
    private final com.marzouki.shop.cart.Cart cart;

    @GetMapping("/login")
    public String login(Authentication authentication) {
        return isSignedIn(authentication) ? "redirect:/" : "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Authentication authentication, Model model) {
        if (isSignedIn(authentication)) {
            return "redirect:/";
        }
        model.addAttribute("form", new RegistrationForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form, BindingResult result,
                           HttpServletRequest request, HttpServletResponse response) {
        if (!result.hasFieldErrors("password") && !result.hasFieldErrors("confirmPassword")
                && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "The passwords do not match");
        }
        if (result.hasErrors()) {
            return "auth/register";
        }
        try {
            User user = users.register(form);
            sessionLogin.start(user, request, response);
            cartStore.signIn(user.getId(), cart);
            return "redirect:/?welcome";
        } catch (ConflictException e) {
            result.rejectValue("email", "taken", e.getMessage());
            return "auth/register";
        }
    }

    private static boolean isSignedIn(Authentication authentication) {
        return authentication != null && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.isAuthenticated();
    }
}