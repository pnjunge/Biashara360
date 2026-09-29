-- V037: Add trial period and subscription validity indexing
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS is_trial BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS subscription_valid_until TIMESTAMPTZ NULL;

CREATE INDEX IF NOT EXISTS idx_businesses_subscription_status
  ON businesses (id, is_active, subscription_enabled, subscription_valid_until);
