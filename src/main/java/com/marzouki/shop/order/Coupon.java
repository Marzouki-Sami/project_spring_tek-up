package com.marzouki.shop.order;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(name = "percent_off", nullable = false)
    private int percentOff;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Why the coupon cannot be used right now, or null when it can. */
    public String problemAt(Instant now) {
        if (!active) {
            return "This coupon is not active";
        }
        if (expiresAt != null && expiresAt.isBefore(now)) {
            return "This coupon has expired";
        }
        if (maxUses != null && usedCount >= maxUses) {
            return "This coupon has been used up";
        }
        return null;
    }
}