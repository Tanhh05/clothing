-- Unified migration file:
-- includes refresh_tokens table and category menu extension fields.

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
