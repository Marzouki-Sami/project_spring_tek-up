package com.marzouki.shop.audit;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One line of the audit log. There are no setters and no update or delete operations: the log only grows. */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "audit_log")
public class AuditEntry {

    public static final String OK = "OK";
    public static final String REJECTED = "REJECTED";
    public static final String ERROR = "ERROR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt = Instant.now();

    @Column(name = "actor_id", updatable = false)
    private Long actorId;

    @Column(name = "actor_email", updatable = false)
    private String actorEmail;

    @Column(nullable = false, updatable = false, length = 80)
    private String action;

    @Column(updatable = false, length = 10)
    private String method;

    @Column(name = "entity_type", updatable = false, length = 40)
    private String entityType;

    @Column(name = "entity_id", updatable = false, length = 40)
    private String entityId;

    @Column(nullable = false, updatable = false, length = 12)
    private String outcome;

    @Column(updatable = false, length = 500)
    private String summary;

    @Column(updatable = false, length = 45)
    private String ip;

    AuditEntry(Long actorId, String actorEmail, String action, String method, String entityType, String entityId,
               String outcome, String summary, String ip) {
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.action = cut(action, 80);
        this.method = method;
        this.entityType = cut(entityType, 40);
        this.entityId = cut(entityId, 40);
        this.outcome = outcome;
        this.summary = cut(summary, 500);
        this.ip = cut(ip, 45);
    }

    private static String cut(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}