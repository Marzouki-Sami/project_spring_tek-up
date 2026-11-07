-- Indexes for the filters, sorts and counts behind the shop page and the back-office lists.
CREATE INDEX idx_products_active_created ON products (active, created_at);
CREATE INDEX idx_variants_product_active_stock ON product_variants (product_id, active, stock);
CREATE INDEX idx_orders_user_created ON orders (user_id, created_at);
CREATE INDEX idx_orders_status_created ON orders (status, created_at);
CREATE INDEX idx_users_role_name ON users (role, last_name, first_name);
CREATE INDEX idx_users_created ON users (created_at);
CREATE INDEX idx_coupons_created ON coupons (created_at);
CREATE INDEX idx_wishlist_user_created ON wishlist_items (user_id, created_at);