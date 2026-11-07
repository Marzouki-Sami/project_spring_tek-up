-- Hand-picked related products, in the order the manager chose.
CREATE TABLE related_products (
    product_id BIGINT NOT NULL,
    related_id BIGINT NOT NULL,
    position   INT    NOT NULL,
    PRIMARY KEY (product_id, related_id),
    CONSTRAINT fk_related_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_related_related FOREIGN KEY (related_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT ck_related_not_self CHECK (product_id <> related_id)
);

-- Customers can opt out of "back in stock" emails in their profile.
ALTER TABLE users ADD COLUMN back_in_stock_alerts BOOLEAN NOT NULL DEFAULT TRUE;

-- One "back in stock" email per person and item per week, however often it sells out and returns.
CREATE TABLE stock_alert_log (
    user_id    BIGINT    NOT NULL,
    variant_id BIGINT    NOT NULL,
    sent_at    TIMESTAMP NOT NULL,
    PRIMARY KEY (user_id, variant_id),
    CONSTRAINT fk_alert_user    FOREIGN KEY (user_id)    REFERENCES users (id)             ON DELETE CASCADE,
    CONSTRAINT fk_alert_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id)  ON DELETE CASCADE
);