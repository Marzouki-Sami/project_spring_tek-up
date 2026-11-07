package com.marzouki.shop.user;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.CUSTOMER;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    /** While this moment is in the future, sign-in is refused. */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    /** The authenticator secret, encrypted. Set only while two-factor authentication is on. */
    @Column(name = "totp_secret")
    private String totpSecret;

    @Column(name = "totp_enabled", nullable = false)
    private boolean totpEnabled;

    /** The last 30-second step a code was accepted for, so a code can never be used twice. */
    @Column(name = "totp_last_step")
    private Long totpLastStep;

    /** Customers can switch off emails about items in their basket or wishlist coming back in stock. */
    @Column(name = "back_in_stock_alerts", nullable = false)
    private boolean backInStockAlerts = true;

    /** Managers and admins can switch low-stock emails off. */
    @Column(name = "low_stock_alerts", nullable = false)
    private boolean lowStockAlerts = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }
}