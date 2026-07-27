-- Add profile_id and access_key columns to cybersource_configs for Hosted Checkout
ALTER TABLE cybersource_configs ADD COLUMN IF NOT EXISTS profile_id VARCHAR(255) DEFAULT '';
ALTER TABLE cybersource_configs ADD COLUMN IF NOT EXISTS access_key VARCHAR(255) DEFAULT '';
