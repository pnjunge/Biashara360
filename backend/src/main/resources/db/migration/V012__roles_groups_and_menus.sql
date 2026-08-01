ALTER TABLE businesses ADD COLUMN enabled_menus TEXT NOT NULL DEFAULT
    'DASHBOARD,POS,HOSPITALITY,INVENTORY,ORDERS,CUSTOMERS,EXPENSES,PAYMENTS,CARD_PAYMENTS,TAX,KRA,SOCIAL,SOCIAL_SETUP,USERS,REPORTS,DOWNLOADS,SETTINGS';

CREATE TABLE access_roles (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(255) NOT NULL DEFAULT '',
    allowed_menus TEXT NOT NULL DEFAULT '',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_access_roles_business_name UNIQUE (business_id, name)
);

CREATE TABLE access_groups (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(255) NOT NULL DEFAULT '',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_access_groups_business_name UNIQUE (business_id, name)
);

CREATE TABLE access_group_roles (
    group_id VARCHAR(36) NOT NULL REFERENCES access_groups(id) ON DELETE CASCADE,
    role_id VARCHAR(36) NOT NULL REFERENCES access_roles(id) ON DELETE CASCADE,
    PRIMARY KEY (group_id, role_id)
);

CREATE TABLE user_access_groups (
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id VARCHAR(36) NOT NULL REFERENCES access_groups(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, group_id)
);

CREATE INDEX idx_access_roles_business ON access_roles (business_id);
CREATE INDEX idx_access_groups_business ON access_groups (business_id);
CREATE INDEX idx_user_access_groups_group ON user_access_groups (group_id);
