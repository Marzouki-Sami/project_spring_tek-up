package com.marzouki.shop.security;

import com.marzouki.shop.cart.Cart;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Gives every page the signed-in user's display name and the cart size for the navigation bar. */
@ControllerAdvice
public class CurrentUserAdvice {

    /** Where Spring stores the session-scoped Cart bean inside the HTTP session. */
    private static final String CART_ATTRIBUTE = "scopedTarget.cart";

    @ModelAttribute("currentUserName")
    String currentUserName(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return authentication.getPrincipal() instanceof AppUserDetails user
                ? user.getFullName()
                : authentication.getName();
    }

    /** Reads the cart only if a session already exists, so browsing never creates one. */
    @ModelAttribute("cartCount")
    int cartCount(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute(CART_ATTRIBUTE) instanceof Cart cart ? cart.itemCount() : 0;
    }
}