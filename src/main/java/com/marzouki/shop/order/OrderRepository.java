package com.marzouki.shop.order;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "user"})
    Optional<Order> findWithItemsById(Long id);

    List<Order> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    @EntityGraph(attributePaths = "user")
    List<Order> findAllByOrderByCreatedAtDescIdDesc();

    @EntityGraph(attributePaths = "user")
    List<Order> findByStatusOrderByCreatedAtDescIdDesc(OrderStatus status);
}