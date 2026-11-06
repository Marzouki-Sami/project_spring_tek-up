package com.marzouki.shop.security;

import com.marzouki.shop.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/** Signs a user into the website session without asking for the password again (after sign-up or a profile edit). */
@Component
public class SessionLogin {

    private final SecurityContextRepository repository = new HttpSessionSecurityContextRepository();

    public void start(User user, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        AppUserDetails details = new AppUserDetails(user);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                details, null, details.getAuthorities()));
        SecurityContextHolder.setContext(context);
        repository.saveContext(context, request, response);
    }
}