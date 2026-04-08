-- =========================
-- DROP ALL (nếu cần reset)
-- =========================
DROP TABLE IF EXISTS notifications, shipments, shipping_methods, inventory_logs,
    coupon_usages, coupons, reviews, wishlist_items, wishlists,
    payments, order_status_history, order_items, orders,
    cart_items, carts, product_images, variant_attribute_values,
    attribute_values, attributes, product_variants, products, categories,
    user_roles, users, roles CASCADE;

-- =========================
-- USERS & ROLES
-- =========================
CREATE TABLE roles (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       password TEXT NOT NULL,
                       full_name VARCHAR(100),
                       phone VARCHAR(20),
                       status VARCHAR(20) DEFAULT 'ACTIVE',
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
                            user_id INT REFERENCES users(id) ON DELETE CASCADE,
                            role_id INT REFERENCES roles(id) ON DELETE CASCADE,
                            PRIMARY KEY (user_id, role_id)
);

-- =========================
-- CATEGORY & PRODUCT
-- =========================
CREATE TABLE categories (
                            id SERIAL PRIMARY KEY,
                            name VARCHAR(100),
                            parent_id INT REFERENCES categories(id)
);

CREATE TABLE products (
                          id SERIAL PRIMARY KEY,
                          name VARCHAR(255),
                          slug VARCHAR(255) UNIQUE,
                          description TEXT,
                          brand VARCHAR(100),
                          category_id INT REFERENCES categories(id),
                          status VARCHAR(20),
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- VARIANT & ATTRIBUTE
-- =========================
CREATE TABLE product_variants (
                                  id SERIAL PRIMARY KEY,
                                  product_id INT REFERENCES products(id) ON DELETE CASCADE,
                                  sku VARCHAR(100) UNIQUE,
                                  price BIGINT,
                                  stock INT,
                                  weight FLOAT,
                                  status VARCHAR(20)
);

CREATE TABLE attributes (
                            id SERIAL PRIMARY KEY,
                            name VARCHAR(50)
);

CREATE TABLE attribute_values (
                                  id SERIAL PRIMARY KEY,
                                  attribute_id INT REFERENCES attributes(id) ON DELETE CASCADE,
                                  value VARCHAR(50)
);

CREATE TABLE variant_attribute_values (
                                          variant_id INT REFERENCES product_variants(id) ON DELETE CASCADE,
                                          attribute_value_id INT REFERENCES attribute_values(id) ON DELETE CASCADE,
                                          PRIMARY KEY (variant_id, attribute_value_id)
);

-- =========================
-- PRODUCT IMAGE
-- =========================
CREATE TABLE product_images (
                                id SERIAL PRIMARY KEY,
                                product_id INT REFERENCES products(id) ON DELETE CASCADE,
                                url TEXT,
                                is_main BOOLEAN DEFAULT FALSE
);

-- =========================
-- CART
-- =========================
CREATE TABLE carts (
                       id SERIAL PRIMARY KEY,
                       user_id INT REFERENCES users(id) ON DELETE CASCADE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cart_items (
                            id SERIAL PRIMARY KEY,
                            cart_id INT REFERENCES carts(id) ON DELETE CASCADE,
                            variant_id INT REFERENCES product_variants(id),
                            quantity INT CHECK (quantity > 0)
);

-- =========================
-- ORDER
-- =========================
CREATE TABLE orders (
                        id SERIAL PRIMARY KEY,
                        user_id INT REFERENCES users(id),
                        total_price BIGINT,
                        status VARCHAR(50),
                        payment_method VARCHAR(50),
                        address TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
                             id SERIAL PRIMARY KEY,
                             order_id INT REFERENCES orders(id) ON DELETE CASCADE,
                             variant_id INT REFERENCES product_variants(id),
                             quantity INT,
                             price BIGINT
);

CREATE TABLE order_status_history (
                                      id SERIAL PRIMARY KEY,
                                      order_id INT REFERENCES orders(id) ON DELETE CASCADE,
                                      status VARCHAR(50),
                                      changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- PAYMENT
-- =========================
CREATE TABLE payments (
                          id SERIAL PRIMARY KEY,
                          order_id INT REFERENCES orders(id) ON DELETE CASCADE,
                          amount BIGINT,
                          method VARCHAR(50),
                          status VARCHAR(50),
                          transaction_code VARCHAR(100),
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- WISHLIST
-- =========================
CREATE TABLE wishlists (
                           id SERIAL PRIMARY KEY,
                           user_id INT REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE wishlist_items (
                                id SERIAL PRIMARY KEY,
                                wishlist_id INT REFERENCES wishlists(id) ON DELETE CASCADE,
                                product_id INT REFERENCES products(id)
);

-- =========================
-- REVIEW
-- =========================
CREATE TABLE reviews (
                         id SERIAL PRIMARY KEY,
                         user_id INT REFERENCES users(id),
                         product_id INT REFERENCES products(id),
                         rating INT CHECK (rating >= 1 AND rating <= 5),
                         comment TEXT,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- COUPON
-- =========================
CREATE TABLE coupons (
                         id SERIAL PRIMARY KEY,
                         code VARCHAR(50) UNIQUE,
                         discount_type VARCHAR(20),
                         discount_value BIGINT,
                         min_order_value BIGINT,
                         max_discount_value BIGINT,
                         quantity INT,
                         used_count INT DEFAULT 0,
                         start_date TIMESTAMP,
                         end_date TIMESTAMP,
                         status VARCHAR(20),
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE coupon_usages (
                               id SERIAL PRIMARY KEY,
                               coupon_id INT REFERENCES coupons(id),
                               user_id INT REFERENCES users(id),
                               order_id INT REFERENCES orders(id),
                               used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- INVENTORY LOG
-- =========================
CREATE TABLE inventory_logs (
                                id SERIAL PRIMARY KEY,
                                variant_id INT REFERENCES product_variants(id),
                                type VARCHAR(20),
                                quantity INT,
                                before_stock INT,
                                after_stock INT,
                                note TEXT,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- SHIPPING
-- =========================
CREATE TABLE shipping_methods (
                                  id SERIAL PRIMARY KEY,
                                  name VARCHAR(100),
                                  price BIGINT,
                                  estimated_days INT
);

CREATE TABLE shipments (
                           id SERIAL PRIMARY KEY,
                           order_id INT REFERENCES orders(id),
                           shipping_method_id INT REFERENCES shipping_methods(id),
                           tracking_code VARCHAR(100),
                           status VARCHAR(50),
                           shipped_at TIMESTAMP,
                           delivered_at TIMESTAMP
);

-- =========================
-- NOTIFICATION
-- =========================
CREATE TABLE notifications (
                               id SERIAL PRIMARY KEY,
                               user_id INT REFERENCES users(id),
                               title VARCHAR(255),
                               content TEXT,
                               type VARCHAR(50),
                               is_read BOOLEAN DEFAULT FALSE,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- INDEX
-- =========================
CREATE INDEX idx_product_name ON products(name);
CREATE INDEX idx_variant_product ON product_variants(product_id);
CREATE INDEX idx_order_user ON orders(user_id);
CREATE INDEX idx_cart_user ON carts(user_id);
CREATE INDEX idx_coupon_code ON coupons(code);

