package com.marzouki.shop.security.mfa;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, Long> {

    Optional<TrustedDevice> findByTokenHashAndUserIdAndExpiresAtAfter(String tokenHash, Long userId, Instant now);

    long countByUserIdAndExpiresAtAfter(Long userId, Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TrustedDevice d where d.userId = :userId")
    void deleteAllForUser(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TrustedDevice d where d.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}