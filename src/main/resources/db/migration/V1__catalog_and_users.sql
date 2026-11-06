CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    phone VARCHAR(30),
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'MANAGER', 'ADMIN'))
);

CREATE TABLE suppliers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company VARCHAR(120) NOT NULL,
    contact_name VARCHAR(120),
    email VARCHAR(254),
    phone VARCHAR(30),
    address VARCHAR(255),
    registration_number VARCHAR(60),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(80) NOT NULL,
    description VARCHAR(500),
    CONSTRAINT uq_categories_title UNIQUE (title)
);

CREATE TABLE subcategories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(80) NOT NULL,
    description VARCHAR(500),
    category_id BIGINT NOT NULL,
    CONSTRAINT fk_subcategories_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT uq_subcategories_title UNIQUE (category_id, title)
);

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(40) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(2000),
    price DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    subcategory_id BIGINT NOT NULL,
    supplier_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT ck_products_price CHECK (price >= 0),
    CONSTRAINT ck_products_stock CHECK (stock >= 0),
    CONSTRAINT fk_products_subcategory FOREIGN KEY (subcategory_id) REFERENCES subcategories (id),
    CONSTRAINT fk_products_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
);

CREATE INDEX idx_products_subcategory ON products (subcategory_id);
CREATE INDEX idx_products_supplier ON products (supplier_id);