CREATE TABLE mpesa_checkout_attempts (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id),
    order_id VARCHAR(36) NOT NULL REFERENCES orders(id),
    checkout_request_id VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO mpesa_checkout_attempts (id, business_id, order_id, checkout_request_id, created_at)
SELECT gen_random_uuid()::text, business_id, id, stk_checkout_request_id, updated_at
FROM orders
WHERE stk_checkout_request_id IS NOT NULL
ON CONFLICT (checkout_request_id) DO NOTHING;

CREATE INDEX idx_mpesa_attempts_order_created
    ON mpesa_checkout_attempts (order_id, created_at DESC);
