package com.marzouki.shop.api;

import com.marzouki.shop.api.dto.AuthDtos.LoginRequest;
import com.marzouki.shop.api.dto.AuthDtos.MeResponse;
import com.marzouki.shop.api.dto.AuthDtos.RefreshRequest;
import com.marzouki.shop.api.dto.AuthDtos.RegisterRequest;
import com.marzouki.shop.api.dto.AuthDtos.TokenResponse;
import com.marzouki.shop.security.JwtService;
import com.marzouki.shop.security.RefreshTokenService;
import com.marzouki.shop.security.RefreshTokenService.Issued;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserService;
import com.marzouki.shop.user.dto.RegistrationForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthenticationManager authentication;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;
    private final UserService users;

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        RegistrationForm form = new RegistrationForm();
        form.setFirstName(request.firstName());
        form.setLastName(request.lastName());
        form.setEmail(request.email());
        form.setPassword(request.password());
        User user = users.register(form);
        return tokensFor(refreshTokens.issue(user));
    }

    @PostMapping("/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication result = authentication.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        User user = users.findByEmail(result.getName());
        return tokensFor(refreshTokens.issue(user));
    }

    @PostMapping("/auth/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return tokensFor(refreshTokens.rotate(request.refreshToken()));
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokens.revoke(request.refreshToken());
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt token) {
        User user = users.get(subjectId(token));
        return new MeResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getRole().name());
    }

    private static Long subjectId(Jwt token) {
        try {
            return Long.valueOf(token.getSubject());
        } catch (NumberFormatException e) {
            throw new BadCredentialsException("Invalid token");
        }
    }

    private TokenResponse tokensFor(Issued issued) {
        return new TokenResponse(jwt.issueAccessToken(issued.user()), issued.value(), "Bearer",
                jwt.accessTtlSeconds());
    }
}