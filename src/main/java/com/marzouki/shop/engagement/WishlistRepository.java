package com.marzouki.shop.engagement;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {

    @EntityGraph(attributePaths = {"product", "product.subcategory", "product.subcategory.category"})
    List<WishlistItem> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Modifying
    long deleteByUserIdAndProductId(Long userId, Long productId);
}