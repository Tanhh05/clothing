-- =========================
-- ROLES + USERS
-- =========================
INSERT INTO roles (name) VALUES
    ('ADMIN'),
    ('STAFF'),
    ('CUSTOMER')
ON CONFLICT (name) DO NOTHING;

INSERT INTO users (username, email, password, full_name, phone, status) VALUES
    ('admin01', 'admin@clothing.local', '$2y$10$XAFBEd2Bo6Y2NykBxqs4Ge6v8ghhC4R8yZ/41Y5m7LG4jpY66nAMW', 'Quản trị hệ thống', '0900000001', 'ACTIVE'),
    ('staff01', 'staff@clothing.local', '$2a$10$demo_hash_staff', 'Nhân viên cửa hàng', '0900000002', 'ACTIVE'),
    ('linh', 'linh@clothing.local', '$2a$10$demo_hash_alice', 'Nguyễn Thảo Linh', '0900000003', 'ACTIVE'),
    ('huy', 'huy@clothing.local', '$2a$10$demo_hash_bob', 'Trần Minh Huy', '0900000004', 'ACTIVE')
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
 JOIN roles r ON
    (u.username = 'admin01' AND r.name = 'ADMIN')
 OR (u.username = 'staff01' AND r.name = 'STAFF')
 OR (u.username = 'linh' AND r.name = 'CUSTOMER')
 OR (u.username = 'huy' AND r.name = 'CUSTOMER')
ON CONFLICT (user_id, role_id) DO NOTHING;

-- =========================
-- CATEGORIES
-- =========================
INSERT INTO categories (name, slug, parent_id) VALUES ('Nam', 'nam', NULL);
INSERT INTO categories (name, slug, parent_id) VALUES ('Nữ', 'nu', NULL);

INSERT INTO categories (name, slug, parent_id)
SELECT 'Áo thun', 'ao-thun', id FROM categories WHERE name = 'Nam' LIMIT 1;
INSERT INTO categories (name, slug, parent_id)
SELECT 'Áo nỉ có mũ', 'ao-ni-co-mu', id FROM categories WHERE name = 'Nam' LIMIT 1;
INSERT INTO categories (name, slug, parent_id)
SELECT 'Váy', 'vay', id FROM categories WHERE name = 'Nữ' LIMIT 1;

-- =========================
-- PRODUCTS
-- =========================
INSERT INTO products (name, slug, description, brand, category_id, status)
SELECT
    'Áo thun cotton cổ điển',
    'ao-thun-cotton-co-dien',
    'Áo thun cotton cơ bản, phù hợp mặc hằng ngày',
    'Mặc Việt',
    c.id,
    'ACTIVE'
FROM categories c
WHERE c.name = 'Áo thun'
LIMIT 1
ON CONFLICT (slug) DO NOTHING;

INSERT INTO products (name, slug, description, brand, category_id, status)
SELECT
    'Áo nỉ có mũ khóa kéo',
    'ao-ni-co-mu-khoa-keo',
    'Áo nỉ mềm, dễ phối cho phong cách năng động',
    'Mặc Việt',
    c.id,
    'ACTIVE'
FROM categories c
WHERE c.name = 'Áo nỉ có mũ'
LIMIT 1
ON CONFLICT (slug) DO NOTHING;

INSERT INTO products (name, slug, description, brand, category_id, status)
SELECT
    'Váy linen mùa hè',
    'vay-linen-mua-he',
    'Váy linen nhẹ, thoáng mát cho ngày nắng',
    'Mặc Việt',
    c.id,
    'ACTIVE'
FROM categories c
WHERE c.name = 'Váy'
LIMIT 1
ON CONFLICT (slug) DO NOTHING;

-- =========================
-- ATTRIBUTES + VALUES
-- =========================
INSERT INTO attributes (name) VALUES ('Màu sắc');
INSERT INTO attributes (name) VALUES ('Kích cỡ');

INSERT INTO attribute_values (attribute_id, value)
SELECT a.id, v.value_text
FROM attributes a
JOIN (
    VALUES ('Màu sắc', 'Đen'),
           ('Màu sắc', 'Trắng'),
           ('Màu sắc', 'Xanh dương'),
           ('Kích cỡ', 'S'),
           ('Kích cỡ', 'M'),
           ('Kích cỡ', 'L')
) AS v(attribute_name, value_text)
ON a.name = v.attribute_name;

-- =========================
-- VARIANTS
-- =========================
INSERT INTO product_variants (product_id, sku, price, stock, weight, status)
SELECT p.id, 'TSHIRT-BLACK-M', 199000, 120, 0.2, 'ACTIVE'
FROM products p WHERE p.slug = 'ao-thun-cotton-co-dien'
ON CONFLICT (sku) DO NOTHING;

INSERT INTO product_variants (product_id, sku, price, stock, weight, status)
SELECT p.id, 'TSHIRT-WHITE-L', 199000, 80, 0.2, 'ACTIVE'
FROM products p WHERE p.slug = 'ao-thun-cotton-co-dien'
ON CONFLICT (sku) DO NOTHING;

INSERT INTO product_variants (product_id, sku, price, stock, weight, status)
SELECT p.id, 'HOODIE-BLACK-M', 499000, 40, 0.6, 'ACTIVE'
FROM products p WHERE p.slug = 'ao-ni-co-mu-khoa-keo'
ON CONFLICT (sku) DO NOTHING;

INSERT INTO product_variants (product_id, sku, price, stock, weight, status)
SELECT p.id, 'DRESS-BLUE-S', 459000, 35, 0.35, 'ACTIVE'
FROM products p WHERE p.slug = 'vay-linen-mua-he'
ON CONFLICT (sku) DO NOTHING;

-- =========================
-- VARIANT ATTRIBUTE LINK
-- =========================
INSERT INTO variant_attribute_values (variant_id, attribute_value_id)
SELECT pv.id, av.id
FROM product_variants pv
JOIN attribute_values av ON av.value = 'Đen'
WHERE pv.sku IN ('TSHIRT-BLACK-M', 'HOODIE-BLACK-M')
ON CONFLICT (variant_id, attribute_value_id) DO NOTHING;

INSERT INTO variant_attribute_values (variant_id, attribute_value_id)
SELECT pv.id, av.id
FROM product_variants pv
JOIN attribute_values av ON av.value = 'Trắng'
WHERE pv.sku = 'TSHIRT-WHITE-L'
ON CONFLICT (variant_id, attribute_value_id) DO NOTHING;

INSERT INTO variant_attribute_values (variant_id, attribute_value_id)
SELECT pv.id, av.id
FROM product_variants pv
JOIN attribute_values av ON av.value = 'Xanh dương'
WHERE pv.sku = 'DRESS-BLUE-S'
ON CONFLICT (variant_id, attribute_value_id) DO NOTHING;

INSERT INTO variant_attribute_values (variant_id, attribute_value_id)
SELECT pv.id, av.id
FROM product_variants pv
JOIN attribute_values av ON av.value = 'M'
WHERE pv.sku IN ('TSHIRT-BLACK-M', 'HOODIE-BLACK-M')
ON CONFLICT (variant_id, attribute_value_id) DO NOTHING;

INSERT INTO variant_attribute_values (variant_id, attribute_value_id)
SELECT pv.id, av.id
FROM product_variants pv
JOIN attribute_values av ON av.value = 'L'
WHERE pv.sku = 'TSHIRT-WHITE-L'
ON CONFLICT (variant_id, attribute_value_id) DO NOTHING;

INSERT INTO variant_attribute_values (variant_id, attribute_value_id)
SELECT pv.id, av.id
FROM product_variants pv
JOIN attribute_values av ON av.value = 'S'
WHERE pv.sku = 'DRESS-BLUE-S'
ON CONFLICT (variant_id, attribute_value_id) DO NOTHING;

-- =========================
-- IMAGES
-- =========================
INSERT INTO product_images (product_id, url, is_main)
SELECT id, 'https://picsum.photos/seed/tshirt/600/800', TRUE
FROM products WHERE slug = 'ao-thun-cotton-co-dien';

INSERT INTO product_images (product_id, url, is_main)
SELECT id, 'https://picsum.photos/seed/hoodie/600/800', TRUE
FROM products WHERE slug = 'ao-ni-co-mu-khoa-keo';

INSERT INTO product_images (product_id, url, is_main)
SELECT id, 'https://picsum.photos/seed/dress/600/800', TRUE
FROM products WHERE slug = 'vay-linen-mua-he';

-- =========================
-- CART + ORDER + PAYMENT
-- =========================
INSERT INTO carts (user_id)
SELECT id FROM users WHERE username = 'linh' LIMIT 1;

INSERT INTO cart_items (cart_id, variant_id, quantity)
SELECT c.id, pv.id, 2
FROM carts c
JOIN users u ON u.id = c.user_id
JOIN product_variants pv ON pv.sku = 'TSHIRT-BLACK-M'
WHERE u.username = 'linh'
LIMIT 1;

INSERT INTO orders (user_id, total_price, status, payment_method, address)
SELECT u.id, 398000, 'CONFIRMED', 'COD', '123 Nguyễn Trãi, TP Hồ Chí Minh'
FROM users u WHERE u.username = 'linh'
LIMIT 1;

INSERT INTO order_items (order_id, variant_id, quantity, price)
SELECT o.id, pv.id, 2, 199000
FROM orders o
JOIN users u ON u.id = o.user_id
JOIN product_variants pv ON pv.sku = 'TSHIRT-BLACK-M'
WHERE u.username = 'linh'
ORDER BY o.id DESC
LIMIT 1;

INSERT INTO order_status_history (order_id, status)
SELECT id, 'CONFIRMED' FROM orders ORDER BY id DESC LIMIT 1;

INSERT INTO payments (order_id, amount, method, status, transaction_code)
SELECT id, 398000, 'COD', 'PENDING', 'COD-DEMO-0001'
FROM orders
ORDER BY id DESC
LIMIT 1;

-- =========================
-- WISHLIST + REVIEW
-- =========================
INSERT INTO wishlists (user_id)
SELECT id FROM users WHERE username = 'huy' LIMIT 1;

INSERT INTO wishlist_items (wishlist_id, product_id)
SELECT w.id, p.id
FROM wishlists w
JOIN users u ON u.id = w.user_id
JOIN products p ON p.slug = 'ao-ni-co-mu-khoa-keo'
WHERE u.username = 'huy'
LIMIT 1;

INSERT INTO reviews (user_id, product_id, rating, comment)
SELECT u.id, p.id, 5, 'Áo mặc rất thoải mái và đúng kích cỡ.'
FROM users u
JOIN products p ON p.slug = 'ao-thun-cotton-co-dien'
WHERE u.username = 'linh'
LIMIT 1;

-- =========================
-- COUPON + USAGE
-- =========================
INSERT INTO coupons (
    code, discount_type, discount_value, min_order_value, max_discount_value,
    quantity, used_count, start_date, end_date, status
) VALUES (
    'CHAOMUNG10', 'PERCENT', 10, 200000, 50000,
    100, 1, NOW() - INTERVAL '1 day', NOW() + INTERVAL '30 days', 'ACTIVE'
);

INSERT INTO coupon_usages (coupon_id, user_id, order_id)
SELECT c.id, u.id, o.id
FROM coupons c
JOIN users u ON u.username = 'linh'
JOIN orders o ON o.user_id = u.id
WHERE c.code = 'CHAOMUNG10'
ORDER BY o.id DESC
LIMIT 1;

-- =========================
-- INVENTORY + SHIPPING + NOTIFICATION
-- =========================
INSERT INTO inventory_logs (variant_id, type, quantity, before_stock, after_stock, note)
SELECT id, 'OUT', 2, 122, 120, 'Tạo đơn hàng'
FROM product_variants
WHERE sku = 'TSHIRT-BLACK-M'
LIMIT 1;

INSERT INTO shipping_methods (name, price, estimated_days) VALUES
    ('Giao hàng tiêu chuẩn', 30000, 3),
    ('Giao hàng nhanh', 50000, 1);

INSERT INTO shipments (order_id, shipping_method_id, tracking_code, status, shipped_at)
SELECT o.id, sm.id, 'VNPOST-DEMO-001', 'SHIPPING', NOW()
FROM orders o
JOIN shipping_methods sm ON sm.name = 'Giao hàng tiêu chuẩn'
ORDER BY o.id DESC
LIMIT 1;

INSERT INTO notifications (user_id, title, content, type, is_read)
SELECT u.id, 'Xác nhận đơn hàng', 'Đơn hàng của bạn đã được xác nhận thành công.', 'ORDER', FALSE
FROM users u
WHERE u.username = 'linh'
LIMIT 1;
