-- Two-factor authentication
ALTER TABLE users ADD COLUMN totp_secret VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN totp_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN totp_last_step BIGINT NULL;

CREATE TABLE recovery_codes (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    code_hash  VARCHAR(64)  NOT NULL,
    used_at    TIMESTAMP    NULL,
    CONSTRAINT fk_recovery_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_recovery_code UNIQUE (user_id, code_hash)
);

CREATE TABLE trusted_devices (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    label       VARCHAR(120) NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    CONSTRAINT fk_trusted_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_trusted_token UNIQUE (token_hash)
);
CREATE INDEX idx_trusted_user ON trusted_devices (user_id);

-- Append-only history of what staff did. No foreign key to users, so the record outlives the account.
CREATE TABLE audit_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    occurred_at   TIMESTAMP    NOT NULL,
    actor_id      BIGINT       NULL,
    actor_email   VARCHAR(254) NULL,
    action        VARCHAR(80)  NOT NULL,
    method        VARCHAR(10)  NULL,
    entity_type   VARCHAR(40)  NULL,
    entity_id     VARCHAR(40)  NULL,
    outcome       VARCHAR(12)  NOT NULL,
    summary       VARCHAR(500) NULL,
    ip            VARCHAR(45)  NULL
);
CREATE INDEX idx_audit_time ON audit_log (occurred_at);
CREATE INDEX idx_audit_actor ON audit_log (actor_id);
CREATE INDEX idx_audit_entity ON audit_log (entity_type, entity_id);