ALTER TABLE businesses ADD COLUMN IF NOT EXISTS max_users INTEGER NOT NULL DEFAULT 2;
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS subscription_valid_until TIMESTAMPTZ NULL;
INSERT INTO system_settings(key,value,updated_at) VALUES
('subscription_user_bands','[{"id":"STARTER","name":"Starter","minUsers":1,"maxUsers":2,"monthlyPrice":500},{"id":"TEAM","name":"Team","minUsers":3,"maxUsers":5,"monthlyPrice":1000},{"id":"GROWTH","name":"Growth","minUsers":6,"maxUsers":10,"monthlyPrice":2000}]',CURRENT_TIMESTAMP)
ON CONFLICT (key) DO NOTHING;
