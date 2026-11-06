package com.marzouki.shop.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByOrderIdOrderByIdAsc(Long orderId);
}