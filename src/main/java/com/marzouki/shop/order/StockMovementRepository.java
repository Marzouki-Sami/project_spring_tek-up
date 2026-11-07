package com.marzouki.shop.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByOrderIdOrderByIdAsc(Long orderId);

    @EntityGraph(attributePaths = "actor")
    @org.springframework.data.jpa.repository.Query("select m from StockMovement m where m.createdAt >= :from and m.createdAt < :to order by m.createdAt desc, m.id desc")
    Page<StockMovement> inRange(@org.springframework.data.repository.query.Param("from") java.time.Instant from,
            @org.springframework.data.repository.query.Param("to") java.time.Instant to, Pageable pageable);

    @EntityGraph(attributePaths = "actor")
    Page<StockMovement> findByVariantIdOrderByCreatedAtDescIdDesc(Long variantId, Pageable pageable);
}