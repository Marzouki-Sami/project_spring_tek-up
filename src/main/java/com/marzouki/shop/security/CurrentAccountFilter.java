package com.marzouki.shop.security;

import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 */
@RequiredArgsConstructor
class CurrentAccountFilter extends OncePerRequestFilter {

    private final UserRepository users;

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
            } else if (current.getRole() != me.getRole()) {
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
        }
        chain.doFilter(request, response);
    }
}