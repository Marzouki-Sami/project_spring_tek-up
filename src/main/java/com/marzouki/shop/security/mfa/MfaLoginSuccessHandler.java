package com.marzouki.shop.security.mfa;

import com.marzouki.shop.security.AppUserDetails;
import com.marzouki.shop.user.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/** After the password is accepted: a browser the user chose to trust skips the code. */
@org.springframework.stereotype.Component
public class MfaLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public static final String VERIFIED = "mfa.verified";

    private final UserRepository users;
    private final TwoFactorService twoFactor;
    private final TrustCookie cookie;

    public MfaLoginSuccessHandler(UserRepository users, TwoFactorService twoFactor, TrustCookie cookie) {
        this.users = users;
        this.twoFactor = twoFactor;
        this.cookie = cookie;
        setDefaultTargetUrl("/");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof AppUserDetails me) {
            users.findById(me.getId()).ifPresent(user -> {
                if (user.isTotpEnabled() && twoFactor.isTrusted(user.getId(), cookie.read(request))) {
                    request.getSession().setAttribute(VERIFIED, Boolean.TRUE);
                }
            });
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}