ALTER TABLE social_channels
    ADD COLUMN IF NOT EXISTS connection_status VARCHAR(20) NOT NULL DEFAULT 'CONNECTED',
    ADD COLUMN IF NOT EXISTS onboarding_method VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN IF NOT EXISTS token_encryption_version INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS registration_pin_encrypted TEXT,
    ADD COLUMN IF NOT EXISTS last_verified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS disconnected_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS social_channels_business_platform_status_idx
    ON social_channels (business_id, platform, connection_status);
