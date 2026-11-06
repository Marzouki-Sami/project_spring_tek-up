package com.marzouki.shop.security;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Gives every page the signed-in user's display name for the navigation bar. */
@ControllerAdvice
public class CurrentUserAdvice {

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