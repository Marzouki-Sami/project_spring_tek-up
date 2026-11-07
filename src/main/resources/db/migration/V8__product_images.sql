-- Images live in file storage; the database only keeps their keys, size and order.
-- The image with the lowest position is the preview shown on cards.
CREATE TABLE product_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    file_key VARCHAR(64) NOT NULL,
    thumb_key VARCHAR(64) NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    alt_text VARCHAR(200) NULL,
    position INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_product_images_file UNIQUE (file_key),
    CONSTRAINT uq_product_images_thumb UNIQUE (thumb_key),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);
CREATE INDEX idx_product_images_product ON product_images (product_id, position);