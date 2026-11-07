package com.marzouki.shop.cart;

import com.marzouki.shop.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * After every basket page request by a signed-in customer, saves the basket to the database. Doing it
 * here keeps the controller free of persistence code and covers every way the basket can change.
 */
@Configuration
@RequiredArgsConstructor
class CartPersistence implements WebMvcConfigurer {

    private static final String CART_ATTRIBUTE = "scopedTarget.cart";

    private final CartStore store;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                        Exception ex) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                HttpSession session = request.getSession(false);
                if (ex != null || session == null || authentication == null
                        || !(authentication.getPrincipal() instanceof AppUserDetails user)) {
                    return;
                }
                if (session.getAttribute(CART_ATTRIBUTE) instanceof Cart cart) {
                    store.save(user.getId(), cart);
                }
            }
        }).addPathPatterns("/cart", "/cart/**");
    }
}