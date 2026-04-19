-- Seed SQL for Clothing Database
-- This script creates tables and inserts sample data based on the entities

-- Create tables

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    phone VARCHAR(20),
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id bigint NOT NULL,
    role_id bigint NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS categories (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name varchar(100),
    slug varchar(255) UNIQUE,
    parent_id bigint,
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    slug VARCHAR(255) UNIQUE,
    parent_id BIGINT,
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name varchar(255),
    slug varchar(255) UNIQUE,
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    slug VARCHAR(255) UNIQUE,
    description TEXT,
    brand VARCHAR(100),
    category_id BIGINT,
    status VARCHAR(20),
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,

CREATE TABLE IF NOT EXISTS product_variants (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id bigint,
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT,
    sku VARCHAR(100) UNIQUE,
    price BIGINT,
    stock INT,
    weight DOUBLE,
    status VARCHAR(20),
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name varchar(255)
);

CREATE TABLE IF NOT EXISTS attribute_values (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255)
);

    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attribute_id BIGINT,
    value VARCHAR(50),
    FOREIGN KEY (variant_id) REFERENCES product_variants(id),
    FOREIGN KEY (attribute_value_id) REFERENCES attribute_values(id)
);

CREATE TABLE IF NOT EXISTS orders (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint,
    variant_id BIGINT NOT NULL,
    attribute_value_id BIGINT NOT NULL,
    sub_total bigint,
    shipping_fee bigint,
    discount_amount bigint,
    coupon_id bigint,
    coupon_code varchar(50),
    status varchar(50),
    payment_method varchar(50),
    shipping_provider varchar(50),
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    total_price BIGINT,
    sub_total BIGINT,
    shipping_fee BIGINT,
    discount_amount BIGINT,
    coupon_id BIGINT,
    coupon_code VARCHAR(50),
    status VARCHAR(50),
    payment_method VARCHAR(50),
    shipping_provider VARCHAR(50),
    shipping_code VARCHAR(100) UNIQUE,
    shipping_status VARCHAR(50),
    shipping_updated_at TIMESTAMP,
    address TEXT,
    created_at TIMESTAMP,
);

CREATE TABLE IF NOT EXISTS banners (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title varchar(255),
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT,
    variant_id BIGINT,
    quantity INT,
    price BIGINT,
    end_at timestamp,
    is_deleted boolean NOT NULL DEFAULT false,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS carts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    image_url VARCHAR(1000) NOT NULL,
    link_url VARCHAR(1000),
    status VARCHAR(20),
    start_at TIMESTAMP,
    end_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    FOREIGN KEY (variant_id) REFERENCES product_variants(id)
);

CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    created_at TIMESTAMP,
    discount_value bigint,
    min_order_value bigint,
    max_discount_value bigint,
    quantity integer,
    used_count integer,
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT,
    variant_id BIGINT,
    quantity INT,
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    coupon_id bigint,
    user_id bigint,
    order_id bigint,
    used_at timestamp,
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) UNIQUE,
    discount_type VARCHAR(20),
    discount_value BIGINT,
    min_order_value BIGINT,
    max_discount_value BIGINT,
    quantity INT,
    used_count INT,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    status VARCHAR(20),
    created_at TIMESTAMP
    created_at timestamp,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE IF NOT EXISTS shipping_methods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT,
    user_id BIGINT,
    order_id BIGINT,
    used_at TIMESTAMP,
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id bigint,
    shipping_method_id bigint,
    tracking_code varchar(100),
    status varchar(50),
    shipped_at timestamp,
    delivered_at timestamp,
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT,
    amount BIGINT,
    method VARCHAR(50),
    status VARCHAR(50),
    transaction_code VARCHAR(100),
    created_at TIMESTAMP,

-- Users
INSERT INTO users (username, email, password, full_name, phone, status) VALUES
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    price BIGINT,
    estimated_days INT
('user2', 'user2@example.com', '$2a$10$8K2L0.HbMmZJjvTpIehSerWl9lj8w6TdoD3YzqHdZidZTNLP3n.dO', 'Jane Smith', '1122334455', 'ACTIVE');

-- User Roles
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT,
    shipping_method_id BIGINT,
    tracking_code VARCHAR(100),
    status VARCHAR(50),
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP,
('user1', 'user1@example.com', '$2a$10$examplehashedpassword', 'John Doe', '0987654321', 'ACTIVE'),
('user2', 'user2@example.com', '$2a$10$examplehashedpassword', 'Jane Smith', '1122334455', 'ACTIVE');
('Cotton T-Shirt', 'cotton-t-shirt', 'Comfortable cotton t-shirt', 'BrandA', 3, 'ACTIVE', NOW()),
('Jeans', 'jeans', 'Blue denim jeans', 'BrandB', 4, 'ACTIVE', NOW()),
('Summer Dress', 'summer-dress', 'Light summer dress', 'BrandC', 5, 'ACTIVE', NOW()),
('Sneakers', 'sneakers', 'Comfortable sneakers', 'BrandD', 6, 'ACTIVE', NOW());

-- Product Variants
INSERT INTO product_variants (product_id, sku, price, stock, weight, status) VALUES
(1, 'TSHIRT-S', 200000, 50, 0.2, 'ACTIVE'),
(1, 'TSHIRT-M', 200000, 50, 0.2, 'ACTIVE'),
(1, 'TSHIRT-L', 200000, 50, 0.2, 'ACTIVE'),
(2, 'JEANS-32', 500000, 30, 0.8, 'ACTIVE'),
(2, 'JEANS-34', 500000, 30, 0.8, 'ACTIVE'),
(3, 'DRESS-S', 300000, 20, 0.3, 'ACTIVE'),
(3, 'DRESS-M', 300000, 20, 0.3, 'ACTIVE'),
(4, 'SNEAKERS-8', 800000, 15, 1.0, 'ACTIVE'),
(4, 'SNEAKERS-9', 800000, 15, 1.0, 'ACTIVE');

-- Attributes
INSERT INTO attributes (name) VALUES ('Size'), ('Color');

-- Attribute Values
INSERT INTO attribute_values (attribute_id, value) VALUES
(1, 'S'), (1, 'M'), (1, 'L'), (1, '32'), (1, '34'), (1, '8'), (1, '9'),
(2, 'White'), (2, 'Blue'), (2, 'Red'), (2, 'Black');

-- Variant Attribute Values
INSERT INTO variant_attribute_values (variant_id, attribute_value_id) VALUES
(1, 1), (1, 8), -- TSHIRT-S White
(2, 2), (2, 8), -- TSHIRT-M White
(3, 3), (3, 8), -- TSHIRT-L White
(4, 4), (4, 9), -- JEANS-32 Blue
(5, 5), (5, 9), -- JEANS-34 Blue
(6, 1), (6, 10), -- DRESS-S Red
(7, 2), (7, 10), -- DRESS-M Red
(8, 6), (8, 11), -- SNEAKERS-8 Black
(9, 7), (9, 11); -- SNEAKERS-9 Black

-- Orders
INSERT INTO orders (user_id, total_price, sub_total, shipping_fee, discount_amount, status, payment_method, shipping_provider, shipping_code, shipping_status, address, created_at) VALUES
(2, 700000, 700000, 30000, 0, 'COMPLETED', 'CREDIT_CARD', 'FedEx', 'FX123456', 'DELIVERED', '123 Main St, City, Country', NOW()),
(3, 1100000, 1100000, 50000, 0, 'PENDING', 'PAYPAL', 'UPS', 'UP789012', 'IN_TRANSIT', '456 Elm St, City, Country', NOW());

-- Order Items
INSERT INTO order_items (order_id, variant_id, quantity, price) VALUES
(1, 1, 1, 200000),
(1, 4, 1, 500000),
(2, 6, 1, 300000),
(2, 8, 1, 800000);

-- Banners
INSERT INTO banners (title, image_url, link_url, status, start_at, end_at, is_deleted, created_at) VALUES
('Summer Sale', 'https://example.com/banner1.jpg', '/products', 'ACTIVE', NOW(), NOW() + INTERVAL '30 days', FALSE, NOW()),
('New Arrivals', 'https://example.com/banner2.jpg', '/categories', 'ACTIVE', NOW(), NOW() + INTERVAL '15 days', FALSE, NOW());

-- Carts
INSERT INTO carts (user_id, created_at) VALUES
(2, NOW()),
(3, NOW());

-- Cart Items
INSERT INTO cart_items (cart_id, variant_id, quantity) VALUES
(1, 2, 2),
(2, 5, 1);

-- Coupons
INSERT INTO coupons (code, discount_type, discount_value, min_order_value, max_discount_value, quantity, used_count, start_date, end_date, status, created_at) VALUES
('SAVE10', 'PERCENTAGE', 10, 500000, 50000, 100, 5, NOW(), NOW() + INTERVAL '60 days', 'ACTIVE', NOW()),
('FLAT50', 'FIXED', 50000, 1000000, 50000, 50, 2, NOW(), NOW() + INTERVAL '30 days', 'ACTIVE', NOW());

-- Payments
INSERT INTO payments (order_id, amount, method, status, transaction_code, created_at) VALUES
('SAVE10', 'PERCENTAGE', 10, 500000, 50000, 100, 5, NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY), 'ACTIVE', NOW()),
('FLAT50', 'FIXED', 50000, 1000000, 50000, 50, 2, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE', NOW());
(1, 700000, 'CREDIT_CARD', 'COMPLETED', 'TXN123456', NOW()),
(2, 1100000, 'PAYPAL', 'PENDING', 'TXN789012', NOW());

-- Shipping Methods
('Summer Sale', 'https://example.com/banner1.jpg', '/products', 'ACTIVE', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), FALSE, NOW()),
('New Arrivals', 'https://example.com/banner2.jpg', '/categories', 'ACTIVE', NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), FALSE, NOW());
('Standard Shipping', 30000, 5),
('Express Shipping', 50000, 2);

-- Shipments
INSERT INTO shipments (order_id, shipping_method_id, tracking_code, status, shipped_at, delivered_at) VALUES
(1, 1, 'FX123456', 'DELIVERED', NOW(), DATE_ADD(NOW(), INTERVAL 3 DAY)),
(1, 1, 'FX123456', 'DELIVERED', NOW(), NOW() + INTERVAL '3 days'),
(2, 2, 'UP789012', 'IN_TRANSIT', NOW(), NULL);
