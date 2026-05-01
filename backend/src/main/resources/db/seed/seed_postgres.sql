-- Seed data for clothing backend (PostgreSQL)
-- This file assumes schema is already created by migrations.

BEGIN;

-- Roles
INSERT INTO roles (name) VALUES ('ROLE_ADMIN')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name) VALUES ('ROLE_USER')
ON CONFLICT (name) DO NOTHING;

-- Admin user (password: Admin@123)
INSERT INTO users (username, email, password, full_name, phone, status)
VALUES (
    'admin',
    'admin@clothing.local',
    '$2a$10$hwb5l9S6gLBlf9wYyx1IIuMVNQfG3v1R4Lw2y6vJxDf9G9xW2m8uG',
    'System Admin',
    '0900000000',
    'ACTIVE'
)
ON CONFLICT (username) DO NOTHING;

-- Link admin role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_ADMIN'
WHERE u.username = 'admin'
ON CONFLICT DO NOTHING;

-- Store settings
INSERT INTO store_settings (setting_key, setting_value)
VALUES
    ('storeName', 'Clothing Store'),
    ('phone', '0900000000'),
    ('email', 'support@clothing.local'),
    ('address', 'Ho Chi Minh City')
ON CONFLICT (setting_key) DO NOTHING;

-- Root categories
INSERT INTO categories (name, slug, page_type, display_order, show_in_menu, status)
VALUES
    ('Áo', 'ao', 'TRANG_DON', 1, TRUE, 'ACTIVE'),
    ('Quần', 'quan', 'TRANG_DON', 2, TRUE, 'ACTIVE'),
    ('Phụ kiện', 'phu-kien', 'TRANG_DON', 3, TRUE, 'ACTIVE')
ON CONFLICT (slug) DO NOTHING;

-- Sample products
INSERT INTO products (name, slug, description, brand, category_id, status, is_deleted, created_at)
SELECT
    'Áo thun basic',
    'ao-thun-basic',
    'Áo thun cotton form regular',
    'Clothing',
    c.id,
    'ACTIVE',
    FALSE,
    CURRENT_TIMESTAMP
FROM categories c
WHERE c.slug = 'ao'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO products (name, slug, description, brand, category_id, status, is_deleted, created_at)
SELECT
    'Quần jeans slim',
    'quan-jeans-slim',
    'Quần jeans co giãn nhẹ',
    'Clothing',
    c.id,
    'ACTIVE',
    FALSE,
    CURRENT_TIMESTAMP
FROM categories c
WHERE c.slug = 'quan'
ON CONFLICT (slug) DO NOTHING;

-- Sample variants
INSERT INTO product_variants (product_id, sku, price, stock, weight, status)
SELECT p.id, 'AO-BASIC-BLACK-M', 199000, 50, 0.25, 'ACTIVE'
FROM products p
WHERE p.slug = 'ao-thun-basic'
ON CONFLICT (sku) DO NOTHING;

INSERT INTO product_variants (product_id, sku, price, stock, weight, status)
SELECT p.id, 'QUAN-JEANS-BLUE-32', 499000, 30, 0.55, 'ACTIVE'
FROM products p
WHERE p.slug = 'quan-jeans-slim'
ON CONFLICT (sku) DO NOTHING;

-- Sample banner
INSERT INTO banners (title, image_url, link_url, status, start_at, end_at, is_deleted, created_at)
VALUES (
    'Khuyến mãi tháng này',
    'https://picsum.photos/1600/500',
    '/catalog/products',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    FALSE,
    CURRENT_TIMESTAMP
)
ON CONFLICT DO NOTHING;

COMMIT;
