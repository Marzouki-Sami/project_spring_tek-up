package com.marzouki.shop.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByProductId(Long productId);

    boolean existsByVariantId(Long variantId);

    @org.springframework.data.jpa.repository.Query("select distinct i.variant.id from OrderItem i where i.variant.id in :ids")
    java.util.Set<Long> findOrderedVariantIds(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);
}