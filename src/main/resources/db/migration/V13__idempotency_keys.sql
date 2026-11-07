-- Remembers which order an Idempotency-Key produced, so a retried request returns the same order
-- instead of placing a second one. A key is scoped to the user who sent it.
CREATE TABLE idempotency_keys (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT      NOT NULL,
    key_value    VARCHAR(64) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    order_id     BIGINT      NOT NULL,
    created_at   TIMESTAMP   NOT NULL,
    CONSTRAINT fk_idem_user  FOREIGN KEY (user_id)  REFERENCES users (id)  ON DELETE CASCADE,
    CONSTRAINT fk_idem_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT uq_idem_user_key UNIQUE (user_id, key_value)
);
CREATE INDEX idx_idem_created ON idempotency_keys (created_at);