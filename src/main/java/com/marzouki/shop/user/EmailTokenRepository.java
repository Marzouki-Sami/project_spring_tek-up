package com.marzouki.shop.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<EmailToken> findByTokenHashAndType(String tokenHash, EmailToken.Type type);

    /** Only the newest link of a kind should work, so older ones are retired when a new one is issued. */
    @Modifying(flushAutomatically = true)
    @Query("update EmailToken t set t.usedAt = :now where t.user.id = :userId and t.type = :type and t.usedAt is null")
    int retireOpen(@Param("userId") Long userId, @Param("type") EmailToken.Type type,
                   @Param("now") java.time.Instant now);
}