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

