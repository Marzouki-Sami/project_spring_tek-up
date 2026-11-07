package com.marzouki.shop.order;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByUserIdAndKeyValue(Long userId, String keyValue);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from IdempotencyKey k where k.createdAt < :before")
    int deleteOlderThan(@Param("before") Instant before);
}