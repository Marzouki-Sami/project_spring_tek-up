package com.marzouki.shop.media;

import com.marzouki.shop.catalog.Product;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    /** Storage key of the large version (at most 1200 px on its longest side). */
    @Column(name = "file_key", nullable = false, unique = true, length = 64)
    private String fileKey;

    /** Storage key of the small version used on product cards (at most 400 px). */
    @Column(name = "thumb_key", nullable = false, unique = true, length = 64)
    private String thumbKey;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(name = "alt_text", length = 200)
    private String altText;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public String getUrl() {
        return "/media/" + fileKey;
    }

    public String getThumbUrl() {
        return "/media/" + thumbKey;
    }
}