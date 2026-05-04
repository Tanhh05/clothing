-- Consolidated migration (single file)
-- Includes: refresh tokens, category extension columns, payment sessions,
-- POS drafts, stock reservations, and legacy demo table cleanup.

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP NULL,
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_revoked
    ON refresh_tokens (user_id, revoked);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at
    ON refresh_tokens (expires_at);

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS subtitle VARCHAR(150),
    ADD COLUMN IF NOT EXISTS external_link VARCHAR(500),
    ADD COLUMN IF NOT EXISTS page_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS short_content VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS display_order INTEGER,
    ADD COLUMN IF NOT EXISTS show_in_menu BOOLEAN,
    ADD COLUMN IF NOT EXISTS status VARCHAR(20);

UPDATE categories
SET page_type = COALESCE(NULLIF(TRIM(page_type), ''), 'TRANG_DON'),
    display_order = COALESCE(display_order, 0),
    show_in_menu = COALESCE(show_in_menu, false),
    status = COALESCE(NULLIF(UPPER(TRIM(status)), ''), 'ACTIVE');

CREATE TABLE IF NOT EXISTS momo_checkout_sessions (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(120) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    request_payload TEXT NOT NULL,
    cart_items_payload TEXT NOT NULL,
    sub_total BIGINT NOT NULL,
    shipping_fee BIGINT NOT NULL,
    discount_amount BIGINT NOT NULL,
    total_price BIGINT NOT NULL,
    coupon_id BIGINT NULL,
    coupon_code VARCHAR(50),
    status VARCHAR(30) NOT NULL,
    payment_transaction_code VARCHAR(120),
    created_order_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP NULL,
    consumed_at TIMESTAMP NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_momo_checkout_sessions_user_status
    ON momo_checkout_sessions (user_id, status);

CREATE INDEX IF NOT EXISTS idx_momo_checkout_sessions_created_order
    ON momo_checkout_sessions (created_order_id);

CREATE TABLE IF NOT EXISTS vnpay_checkout_sessions (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(120) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    request_payload TEXT NOT NULL,
    cart_items_payload TEXT NOT NULL,
    sub_total BIGINT NOT NULL,
    shipping_fee BIGINT NOT NULL,
    discount_amount BIGINT NOT NULL,
    total_price BIGINT NOT NULL,
    coupon_id BIGINT NULL,
    coupon_code VARCHAR(50),
    status VARCHAR(30) NOT NULL,
    payment_transaction_code VARCHAR(120),
    created_order_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP NULL,
    consumed_at TIMESTAMP NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vnpay_checkout_sessions_user_status
    ON vnpay_checkout_sessions (user_id, status);

CREATE INDEX IF NOT EXISTS idx_vnpay_checkout_sessions_created_order
    ON vnpay_checkout_sessions (created_order_id);

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS note TEXT;

CREATE TABLE IF NOT EXISTS pos_drafts (
    id BIGSERIAL PRIMARY KEY,
    admin_user_id BIGINT NOT NULL,
    terminal_id VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pos_drafts_admin_terminal UNIQUE (admin_user_id, terminal_id),
    CONSTRAINT fk_pos_drafts_admin_user
        FOREIGN KEY (admin_user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pos_drafts_admin_user
    ON pos_drafts (admin_user_id);

CREATE TABLE IF NOT EXISTS stock_reservations (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_stock_reservation_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_reservation_variant
        FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_stock_reservation_variant_status_exp
    ON stock_reservations (variant_id, status, expires_at);

CREATE INDEX IF NOT EXISTS idx_stock_reservation_order_status
    ON stock_reservations (order_id, status);

DROP TABLE IF EXISTS demo_articles;
DROP TABLE IF EXISTS vue_admin_roles;
