package com.marzouki.shop.security.mfa;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, Long> {

    Optional<RecoveryCode> findByUserIdAndCodeHashAndUsedAtIsNull(Long userId, String codeHash);

    long countByUserIdAndUsedAtIsNull(Long userId);

    /** Marks a code used only if nobody else did first, so one code cannot sign in twice. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RecoveryCode c set c.usedAt = :now where c.id = :id and c.usedAt is null")
    int markUsed(@Param("id") Long id, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from RecoveryCode c where c.userId = :userId")
    void deleteAllForUser(@Param("userId") Long userId);
}