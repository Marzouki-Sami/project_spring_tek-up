package com.marzouki.shop.settings;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Shop-wide settings. There is exactly one row. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shop_settings")
public class ShopSettings {

    static final int ID = 1;

    @Id
    private Integer id = ID;

    @Column(name = "shop_name", nullable = false, length = 80)
    private String shopName;

    @Column(name = "accent_color", nullable = false, length = 7)
    private String accentColor;

    @Column(name = "footer_text", nullable = false, length = 300)
    private String footerText;

    @Column(name = "support_email", length = 254)
    private String supportEmail;

    @Column(name = "company_address", length = 300)
    private String companyAddress;

    @Column(name = "vat_number", length = 40)
    private String vatNumber;

    @Column(name = "vat_percent", nullable = false, precision = 4, scale = 1)
    private BigDecimal vatPercent;

    @Column(name = "invoice_note", length = 300)
    private String invoiceNote;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}