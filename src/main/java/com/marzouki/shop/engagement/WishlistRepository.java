package com.marzouki.shop.engagement;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {

    @EntityGraph(attributePaths = {"product", "product.subcategory", "product.subcategory.category"})
    List<WishlistItem> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"product", "product.subcategory", "product.subcategory.category"})
    @org.springframework.data.jpa.repository.Query("select w from WishlistItem w where w.user.id = :userId and (:q = '' or lower(w.product.name) like :q escape '\\')")
    org.springframework.data.domain.Page<WishlistItem> search(@org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable pageable);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Modifying
    long deleteByUserIdAndProductId(Long userId, Long productId);
}