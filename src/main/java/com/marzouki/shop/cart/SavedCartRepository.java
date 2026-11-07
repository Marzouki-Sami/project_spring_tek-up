package com.marzouki.shop.cart;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavedCartRepository extends JpaRepository<SavedCart, Long> {

    @EntityGraph(attributePaths = "quantities")
    Optional<SavedCart> findByUserId(Long userId);

    @Modifying
    @Query("delete from SavedCart c where c.updatedAt < :before")
    int deleteStale(@Param("before") Instant before);
}