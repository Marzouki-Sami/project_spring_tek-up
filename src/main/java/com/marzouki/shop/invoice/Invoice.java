package com.marzouki.shop.invoice;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** An issued invoice. Its details never change after issue. */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private Long orderId;

    @Column(name = "invoice_number", nullable = false, updatable = false, length = 30)
    private String number;

    @Column(name = "invoice_year", nullable = false, updatable = false)
    private int year;

    @Column(name = "sequence_number", nullable = false, updatable = false)
    private int sequence;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt = Instant.now();

    @Column(name = "seller_name", nullable = false, updatable = false, length = 80)
    private String sellerName;

    @Column(name = "seller_address", updatable = false, length = 300)
    private String sellerAddress;

    @Column(name = "seller_vat_number", updatable = false, length = 40)
    private String sellerVatNumber;

    @Column(name = "vat_percent", nullable = false, updatable = false, precision = 4, scale = 1)
    private BigDecimal vatPercent;

    @Column(updatable = false, length = 300)
    private String note;

    Invoice(Long orderId, int year, int sequence, String sellerName, String sellerAddress, String sellerVatNumber,
            BigDecimal vatPercent, String note) {
        this.orderId = orderId;
        this.year = year;
        this.sequence = sequence;
        this.number = String.format("INV-%d-%06d", year, sequence);
        this.sellerName = sellerName;
        this.sellerAddress = sellerAddress;
        this.sellerVatNumber = sellerVatNumber;
        this.vatPercent = vatPercent;
        this.note = note;
    }
}