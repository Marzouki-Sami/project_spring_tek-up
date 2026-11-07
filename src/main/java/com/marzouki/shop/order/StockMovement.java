package com.marzouki.shop.order;

import com.marzouki.shop.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One change to a variant's stock. The ledger of these explains the number on the shelf. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stock_movements")
public class StockMovement {

    public enum Reason {
        ORDER("Order"), CANCELLATION("Order cancelled"), INITIAL("Initial stock"), RECOUNT("Recount"),
        SUPPLIER_DELIVERY("Supplier delivery"), DAMAGED("Damaged or lost"), RETURN("Customer return"),
        OTHER("Other"), ADJUSTMENT("Adjustment");

        private final String label;

        Reason(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        /** The reasons a person may pick when changing stock by hand. */
        public static java.util.List<Reason> manual() {
            return java.util.List.of(SUPPLIER_DELIVERY, RECOUNT, RETURN, DAMAGED, OTHER);
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(nullable = false)
    private int delta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Reason reason;

    @Column(length = 200)
    private String note;

    @Column(name = "order_id")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public StockMovement(Long productId, Long variantId, int delta, Reason reason, Long orderId) {
        this.productId = productId;
        this.variantId = variantId;
        this.delta = delta;
        this.reason = reason;
        this.orderId = orderId;
    }
}