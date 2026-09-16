ALTER TABLE orders ADD COLUMN IF NOT EXISTS billing_owner_user_id VARCHAR(36) NULL REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS billing_owner_user_id VARCHAR(36) NULL REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_orders_billing_owner ON orders(billing_owner_user_id);
CREATE INDEX IF NOT EXISTS idx_payments_billing_owner ON payments(billing_owner_user_id);