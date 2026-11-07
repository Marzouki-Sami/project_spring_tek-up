package com.marzouki.shop.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stock keeping unit: the store's own unique code for the product. */
    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Inactive products are hidden from customers but keep their order history. */
    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subcategory_id")
    private Subcategory subcategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    /** Null for a simple product; otherwise says which options the variants are built from. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_type_id")
    private ProductType productType;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position, id")
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position, id")
    private List<com.marzouki.shop.media.ProductImage> images = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** The image shown on cards and first in the gallery, or null when the product has none. */
    public com.marzouki.shop.media.ProductImage getPrimaryImage() {
        return images.isEmpty() ? null : images.get(0);
    }

    public ProductVariant addVariant(String sku, int stock) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(this);
        variant.setSku(sku);
        variant.setStock(stock);
        variant.setPosition(variants.size());
        variants.add(variant);
        return variant;
    }

    /** Units on hand across the variants that can be bought. */
    public int getStock() {
        return variants.stream().filter(ProductVariant::isActive).mapToInt(ProductVariant::getStock).sum();
    }

    public boolean isOptioned() {
        return productType != null;
    }

    /** The only variant of a simple product. */
    public ProductVariant getDefaultVariant() {
        return variants.isEmpty() ? null : variants.get(0);
    }

    public List<ProductVariant> getActiveVariants() {
        return variants.stream().filter(ProductVariant::isActive).toList();
    }

    /** The lowest price a customer could pay: the product price or the cheapest variant override. */
    public java.math.BigDecimal getFromPrice() {
        return getActiveVariants().stream().map(ProductVariant::getEffectivePrice)
                .min(Comparator.naturalOrder()).orElse(price);
    }

    public boolean isPriceVaries() {
        return getActiveVariants().stream().map(ProductVariant::getEffectivePrice).distinct().count() > 1;
    }
}