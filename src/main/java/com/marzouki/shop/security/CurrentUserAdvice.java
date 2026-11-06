package com.marzouki.shop.security;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Gives every page the signed-in user's display name for the navigation bar. */
@ControllerAdvice
public class CurrentUserAdvice {

    private final com.marzouki.shop.cart.Cart cart;

    CurrentUserAdvice(com.marzouki.shop.cart.Cart cart) {
        this.cart = cart;
    }

    @ModelAttribute("cartCount")
    int cartCount() {
        return cart.itemCount();
    }

    @ModelAttribute("currentUserName")
    String currentUserName(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return authentication.getPrincipal() instanceof AppUserDetails user
                ? user.getFullName()
                : authentication.getName();
    }
}