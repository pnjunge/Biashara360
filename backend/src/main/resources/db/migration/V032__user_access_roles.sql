CREATE TABLE IF NOT EXISTS user_access_roles (
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id VARCHAR(36) NOT NULL REFERENCES access_roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_user_access_roles_role ON user_access_roles (role_id);
