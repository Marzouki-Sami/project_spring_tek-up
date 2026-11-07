package com.marzouki.shop.common;

import com.marzouki.shop.security.AppUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** Who is doing the current request, for history records. Null outside a request (seed data, jobs). */
public final class CurrentActor {

    private CurrentActor() {
    }

    public static Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserDetails user) {
            return user.getId();
        }
        if (principal instanceof Jwt jwt) {
            try {
                return Long.valueOf(jwt.getSubject());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}