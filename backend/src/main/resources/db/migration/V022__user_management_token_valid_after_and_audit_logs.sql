ALTER TABLE users ADD COLUMN IF NOT EXISTS token_valid_after TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    actor_user_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    target_user_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45),
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_business_id ON audit_logs(business_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);
