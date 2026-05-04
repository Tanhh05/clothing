CREATE TABLE IF NOT EXISTS momo_ipn_logs (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(120),
    request_id VARCHAR(120),
    trans_id VARCHAR(120),
    result_code INTEGER,
    message VARCHAR(500),
    raw_payload TEXT,
    process_status VARCHAR(20),
    process_message VARCHAR(500),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_momo_ipn_logs_order_id ON momo_ipn_logs (order_id);
CREATE INDEX IF NOT EXISTS idx_momo_ipn_logs_created_at ON momo_ipn_logs (created_at);
