-- Accounts that existed before email verification was enforced are trusted.
UPDATE users SET email_verified = TRUE;

CREATE TABLE email_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(12) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_email_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_email_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_email_tokens_type CHECK (type IN ('VERIFY', 'RESET'))
);
CREATE INDEX idx_email_tokens_user ON email_tokens (user_id);