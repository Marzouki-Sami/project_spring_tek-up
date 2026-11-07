-- A signed-in customer's basket is kept here, so it survives sign-out and works from any device.
CREATE TABLE carts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coupon_code VARCHAR(40) NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_carts_user UNIQUE (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_carts_updated ON carts (updated_at);

CREATE TABLE cart_items (
    cart_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (cart_id, variant_id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE,
    CONSTRAINT ck_cart_items_quantity CHECK (quantity > 0)
);

-- Saved delivery addresses. The map position is optional.
CREATE TABLE addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    label VARCHAR(40) NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    phone VARCHAR(30) NULL,
    street VARCHAR(200) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(80) NOT NULL,
    latitude DECIMAL(9, 6) NULL,
    longitude DECIMAL(9, 6) NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_addresses_lat CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_addresses_lon CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);
CREATE INDEX idx_addresses_user ON addresses (user_id);

-- Orders keep a copy of where they were sent, including the pin if one was chosen.
ALTER TABLE orders ADD COLUMN phone VARCHAR(30) NULL;
ALTER TABLE orders ADD COLUMN latitude DECIMAL(9, 6) NULL;
ALTER TABLE orders ADD COLUMN longitude DECIMAL(9, 6) NULL;