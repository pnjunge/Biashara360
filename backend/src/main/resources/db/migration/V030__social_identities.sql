CREATE TABLE social_identities (
    provider VARCHAR(16) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (provider, subject)
);
CREATE INDEX social_identities_user_id_idx ON social_identities(user_id);
