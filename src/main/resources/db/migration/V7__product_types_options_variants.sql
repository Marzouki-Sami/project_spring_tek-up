-- A product type (for example "Clothing") says which kinds of options its products come in
-- (for example Size and Color). Option types are shared, so Size can serve several product types.
CREATE TABLE option_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    display VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_option_types_name UNIQUE (name),
    CONSTRAINT ck_option_types_display CHECK (display IN ('TEXT', 'COLOR'))
);

CREATE TABLE option_values (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    option_type_id BIGINT NOT NULL,
    label VARCHAR(60) NOT NULL,
    color_hex VARCHAR(7) NULL,
    position INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_option_values_label UNIQUE (option_type_id, label),
    CONSTRAINT fk_option_values_type FOREIGN KEY (option_type_id) REFERENCES option_types (id) ON DELETE CASCADE
);

CREATE TABLE product_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_product_types_name UNIQUE (name)
);

CREATE TABLE product_type_options (
    product_type_id BIGINT NOT NULL,
    option_type_id BIGINT NOT NULL,
    PRIMARY KEY (product_type_id, option_type_id),
    CONSTRAINT fk_pto_product_type FOREIGN KEY (product_type_id) REFERENCES product_types (id) ON DELETE CASCADE,
    CONSTRAINT fk_pto_option_type FOREIGN KEY (option_type_id) REFERENCES option_types (id)
);

ALTER TABLE products ADD COLUMN product_type_id BIGINT NULL;
ALTER TABLE products ADD CONSTRAINT fk_products_type FOREIGN KEY (product_type_id) REFERENCES product_types (id);

-- Every product has at least one variant. A product without options has a single default variant;
-- stock, SKU and an optional price override live on the variant.
CREATE TABLE product_variants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sku VARCHAR(60) NOT NULL,
    price DECIMAL(10, 2) NULL,
    stock INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    position INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_product_variants_sku UNIQUE (sku),
    CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT ck_product_variants_stock CHECK (stock >= 0),
    CONSTRAINT ck_product_variants_price CHECK (price IS NULL OR price >= 0)
);
CREATE INDEX idx_product_variants_product ON product_variants (product_id);
CREATE INDEX idx_product_variants_stock ON product_variants (stock);

CREATE TABLE variant_option_values (
    variant_id BIGINT NOT NULL,
    option_value_id BIGINT NOT NULL,
    PRIMARY KEY (variant_id, option_value_id),
    CONSTRAINT fk_vov_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE,
    CONSTRAINT fk_vov_value FOREIGN KEY (option_value_id) REFERENCES option_values (id)
);

INSERT INTO product_variants (product_id, sku, price, stock, active, position, created_at)
SELECT id, sku, NULL, stock, TRUE, 0, created_at FROM products;

ALTER TABLE products DROP CONSTRAINT ck_products_stock;
ALTER TABLE products DROP COLUMN stock;

ALTER TABLE stock_movements ADD COLUMN variant_id BIGINT NULL;
ALTER TABLE stock_movements ADD COLUMN note VARCHAR(200) NULL;
ALTER TABLE stock_movements ADD COLUMN actor_id BIGINT NULL;
UPDATE stock_movements SET variant_id =
    (SELECT MIN(v.id) FROM product_variants v WHERE v.product_id = stock_movements.product_id);
ALTER TABLE stock_movements ADD CONSTRAINT fk_stock_movements_variant
    FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE;
ALTER TABLE stock_movements ADD CONSTRAINT fk_stock_movements_actor
    FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL;
CREATE INDEX idx_stock_movements_variant ON stock_movements (variant_id, created_at);

ALTER TABLE order_items ADD COLUMN variant_id BIGINT NULL;
ALTER TABLE order_items ADD COLUMN variant_label VARCHAR(200) NULL;
UPDATE order_items SET variant_id =
    (SELECT MIN(v.id) FROM product_variants v WHERE v.product_id = order_items.product_id);
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_variant
    FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE SET NULL;
-- Variant SKUs can be longer than product SKUs.
ALTER TABLE order_items MODIFY sku VARCHAR(60) NOT NULL;
