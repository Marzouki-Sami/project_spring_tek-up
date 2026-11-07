package com.marzouki.shop.cart;

import com.marzouki.shop.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A signed-in customer's basket as stored in the database. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "carts")
public class SavedCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "coupon_code", length = 40)
    private String couponCode;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @ElementCollection
    @CollectionTable(name = "cart_items", joinColumns = @JoinColumn(name = "cart_id"))
    @MapKeyColumn(name = "variant_id")
    @Column(name = "quantity")
    private Map<Long, Integer> quantities = new LinkedHashMap<>();
}