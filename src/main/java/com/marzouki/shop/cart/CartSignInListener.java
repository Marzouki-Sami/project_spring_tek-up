package com.marzouki.shop.cart;

import com.marzouki.shop.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** Merges the guest basket into the customer's saved one when they sign in through the login form. */
@Component
@RequiredArgsConstructor
class CartSignInListener {

    private final CartStore store;
    private final Cart sessionCart;

    @EventListener
    void onSignIn(InteractiveAuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof AppUserDetails user) {
            store.signIn(user.getId(), sessionCart);
        }
    }
}