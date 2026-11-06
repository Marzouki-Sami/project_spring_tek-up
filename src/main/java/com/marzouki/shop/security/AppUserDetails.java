package com.marzouki.shop.security;

import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import java.io.Serial;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** The signed-in user as Spring Security sees it. Holds no entity, so it is safe to keep in the session. */
@Getter
public class AppUserDetails implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String email;
    private final String fullName;
    private final Role role;
    private final String passwordHash;
    private final boolean enabled;
    private final Instant lockedUntil;

    public AppUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.role = user.getRole();
        this.passwordHash = user.getPasswordHash();
        this.enabled = user.isEnabled();
        this.lockedUntil = user.getLockedUntil();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || !lockedUntil.isAfter(Instant.now());
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}