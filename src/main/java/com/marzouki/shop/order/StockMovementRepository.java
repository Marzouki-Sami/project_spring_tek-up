package com.marzouki.shop.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByOrderIdOrderByIdAsc(Long orderId);

    @EntityGraph(attributePaths = "actor")
    Page<StockMovement> findByVariantIdOrderByCreatedAtDescIdDesc(Long variantId, Pageable pageable);
}