package com.marzouki.shop.security.mfa;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** The "trust this device" cookie: random, HttpOnly, and useless without the matching server-side record. */
@Component
public class TrustCookie {

    static final String NAME = "shop_device";

    private final boolean secure;

    TrustCookie(@Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
        this.secure = secure;
    }

    public String read(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    void write(HttpServletResponse response, String token, int days) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(NAME, token).httpOnly(true).secure(secure)
                .sameSite("Lax").path("/").maxAge(Duration.ofDays(days)).build().toString());
    }

    void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(NAME, "").httpOnly(true).secure(secure)
                .sameSite("Lax").path("/").maxAge(0).build().toString());
    }
}