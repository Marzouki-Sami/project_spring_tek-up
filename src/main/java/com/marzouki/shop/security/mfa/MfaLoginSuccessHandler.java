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

    /** Session attribute holding the id of the user who passed the second step in this session. */
    public static final String VERIFIED = "mfa.verified";

    public static boolean isVerified(jakarta.servlet.http.HttpSession session, Long userId) {
        return session != null && userId != null && userId.equals(session.getAttribute(VERIFIED));
    }

    public static void markVerified(jakarta.servlet.http.HttpSession session, Long userId) {
        session.setAttribute(VERIFIED, userId);
    }

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
        // Session-fixation protection keeps attributes, so a flag left by an earlier user of this session must go.
        request.getSession().removeAttribute(VERIFIED);
        if (authentication.getPrincipal() instanceof AppUserDetails me) {
            users.findById(me.getId()).ifPresent(user -> {
                if (user.isTotpEnabled() && twoFactor.isTrusted(user.getId(), cookie.read(request))) {
                    markVerified(request.getSession(), user.getId());
                }
            });
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}