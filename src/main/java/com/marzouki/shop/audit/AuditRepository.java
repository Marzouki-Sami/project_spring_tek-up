package com.marzouki.shop.audit;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditRepository extends JpaRepository<AuditEntry, Long> {

    @Query("select a from AuditEntry a where (:q = '' or lower(a.action) like :q escape '\\' "
            + "or lower(a.summary) like :q escape '\\' or lower(a.actorEmail) like :q escape '\\' "
            + "or a.entityId = :exact) "
            + "and (:outcome = '' or a.outcome = :outcome) and a.occurredAt >= :from and a.occurredAt < :to "
            + "order by a.occurredAt desc, a.id desc")
    Page<AuditEntry> search(@Param("q") String q, @Param("exact") String exact, @Param("outcome") String outcome,
                            @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}