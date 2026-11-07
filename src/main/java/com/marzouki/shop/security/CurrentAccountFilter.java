package com.marzouki.shop.security;

import com.marzouki.shop.security.mfa.MfaLoginSuccessHandler;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Keeps a signed-in website session in step with the account. The session holds a snapshot taken at
 * login, so on every page request the account is looked up again: a disabled or deleted account, or a
 * changed password, ends the session, and a changed role takes effect immediately.
 *
 * <p>It also enforces the second sign-in step. A session that has not passed two-factor verification can
 * only reach the two-factor pages and sign-out. "Not verified" is the default, so any other way of
 * starting a session stays locked until the code is entered.
 */
@RequiredArgsConstructor
class CurrentAccountFilter extends OncePerRequestFilter {

    private final UserRepository users;
    private final boolean requireStaff2fa;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/webjars/")
                || path.startsWith("/images/") || path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() != null && context.getAuthentication().getPrincipal() instanceof AppUserDetails me) {
            User current = users.findById(me.getId()).orElse(null);
            if (current == null || !current.isEnabled() || !current.getPasswordHash().equals(me.getPasswordHash())) {
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) {
                    request.getSession(false).invalidate();
                }
            } else {
                if (current.getRole() != me.getRole()) {
                    refresh(request, current);
                }
                if (secondStepMissing(request, current)) {
                    response.sendRedirect(request.getContextPath() + (current.isTotpEnabled() ? "/2fa/verify" : "/2fa/setup"));
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private void refresh(HttpServletRequest request, User current) {
        AppUserDetails fresh = new AppUserDetails(current);
        SecurityContext refreshed = SecurityContextHolder.createEmptyContext();
        refreshed.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                fresh, null, fresh.getAuthorities()));
        SecurityContextHolder.setContext(refreshed);
        if (request.getSession(false) != null) {
            request.getSession(false).setAttribute(
                    org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    refreshed);
        }
    }

    private boolean secondStepMissing(HttpServletRequest request, User current) {
        boolean needed = current.isTotpEnabled() || (requireStaff2fa && current.getRole().isStaff());
        if (!needed) {
            return false;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/2fa/") || path.equals("/logout") || path.startsWith("/error")) {
            return false;
        }
        HttpSession session = request.getSession(false);
        return session == null || !Boolean.TRUE.equals(session.getAttribute(MfaLoginSuccessHandler.VERIFIED));
    }
}
