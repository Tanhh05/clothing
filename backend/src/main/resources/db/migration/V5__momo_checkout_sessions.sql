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
