CREATE TABLE password_reset_links (
    token_hash VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX password_reset_links_user_idx ON password_reset_links(user_id);
