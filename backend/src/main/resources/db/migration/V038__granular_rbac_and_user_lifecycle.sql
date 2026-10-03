-- V038: Granular RBAC, Role-Permission Matrix, User Lifecycle, and Multi-Branch Assignments

-- 1. Extend users table with lifecycle columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_by_user_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL;

UPDATE users SET status = 'DISABLED' WHERE is_active = FALSE;
UPDATE users SET status = 'LOCKED' WHERE pin_locked_until IS NOT NULL AND pin_locked_until > NOW();

-- 2. Granular Permissions catalog
CREATE TABLE IF NOT EXISTS permissions (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    module VARCHAR(40) NOT NULL,
    action VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_permissions_module ON permissions(module);
CREATE INDEX IF NOT EXISTS idx_permissions_code ON permissions(code);

-- 3. Seed system-wide granular permissions
INSERT INTO permissions (id, code, module, action, name, description) VALUES
    ('perm_users_view', 'users.view', 'USERS', 'VIEW', 'View Users', 'View merchant user directory and profiles'),
    ('perm_users_create', 'users.create', 'USERS', 'CREATE', 'Create Users', 'Invite and create new team members'),
    ('perm_users_update', 'users.update', 'USERS', 'UPDATE', 'Update Users', 'Edit user profiles and credentials'),
    ('perm_users_disable', 'users.disable', 'USERS', 'UPDATE', 'Activate / Disable Users', 'Enable or deactivate user accounts'),
    ('perm_users_unlock', 'users.unlock', 'USERS', 'UPDATE', 'Unlock Users', 'Unlock accounts locked from invalid attempts'),
    ('perm_users_reset_password', 'users.reset_password', 'USERS', 'UPDATE', 'Reset User Password', 'Reset user login passwords'),
    ('perm_users_reset_pin', 'users.reset_pin', 'USERS', 'UPDATE', 'Set / Reset Staff PIN', 'Manage POS quick-switch PINs'),

    ('perm_roles_view', 'roles.view', 'ROLES', 'VIEW', 'View Roles', 'View role definitions and assigned permissions'),
    ('perm_roles_create', 'roles.create', 'ROLES', 'CREATE', 'Create Roles', 'Create new custom access roles'),
    ('perm_roles_update', 'roles.update', 'ROLES', 'UPDATE', 'Update Roles', 'Modify roles and permission sets'),
    ('perm_roles_assign', 'roles.assign', 'ROLES', 'UPDATE', 'Assign Roles', 'Assign roles to team members'),

    ('perm_orders_view', 'orders.view', 'ORDERS', 'VIEW', 'View Orders', 'View customer sales orders and tickets'),
    ('perm_orders_create', 'orders.create', 'ORDERS', 'CREATE', 'Create Orders', 'Take orders and process checkouts'),
    ('perm_orders_update', 'orders.update', 'ORDERS', 'UPDATE', 'Update Orders', 'Modify items and statuses of orders'),
    ('perm_orders_cancel', 'orders.cancel', 'ORDERS', 'CANCEL', 'Cancel Orders', 'Void or cancel active orders'),
    ('perm_orders_refund', 'orders.refund', 'ORDERS', 'REFUND', 'Refund Orders', 'Issue refunds on settled orders'),

    ('perm_products_view', 'products.view', 'PRODUCTS', 'VIEW', 'View Products', 'Browse catalog and pricing'),
    ('perm_products_create', 'products.create', 'PRODUCTS', 'CREATE', 'Create Products', 'Add new products to catalog'),
    ('perm_products_update', 'products.update', 'PRODUCTS', 'UPDATE', 'Update Products', 'Edit product pricing and details'),
    ('perm_products_delete', 'products.delete', 'PRODUCTS', 'DELETE', 'Delete Products', 'Remove products from catalog'),

    ('perm_inventory_view', 'inventory.view', 'INVENTORY', 'VIEW', 'View Inventory', 'Monitor stock levels and movements'),
    ('perm_inventory_adjust', 'inventory.adjust', 'INVENTORY', 'ADJUST', 'Adjust Inventory', 'Perform stock reconciliations and adjustments'),

    ('perm_payments_view', 'payments.view', 'PAYMENTS', 'VIEW', 'View Payments', 'View transactions, settlements and receipts'),
    ('perm_payments_refund', 'payments.refund', 'PAYMENTS', 'REFUND', 'Refund Payments', 'Initiate payment refunds'),

    ('perm_reports_sales', 'reports.sales', 'REPORTS', 'VIEW', 'Sales Reports', 'Access sales and revenue analytics'),
    ('perm_reports_inventory', 'reports.inventory', 'REPORTS', 'VIEW', 'Inventory Reports', 'Access stock valuation and audit reports'),
    ('perm_reports_financial', 'reports.financial', 'REPORTS', 'VIEW', 'Financial Reports', 'Access P&L, expenses and tax reports'),

    ('perm_settings_view', 'settings.view', 'SETTINGS', 'VIEW', 'View Settings', 'View business profile and store configurations'),
    ('perm_settings_update', 'settings.update', 'SETTINGS', 'UPDATE', 'Update Settings', 'Modify business configuration and policies')
ON CONFLICT (code) DO NOTHING;

-- 4. Role-Permission mappings
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id VARCHAR(36) NOT NULL REFERENCES access_roles(id) ON DELETE CASCADE,
    permission_id VARCHAR(36) NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX IF NOT EXISTS idx_role_permissions_role ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission ON role_permissions(permission_id);

-- 5. User Multi-Branch assignments
CREATE TABLE IF NOT EXISTS user_branches (
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    branch_id VARCHAR(36) NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (user_id, branch_id)
);

CREATE INDEX IF NOT EXISTS idx_user_branches_user ON user_branches(user_id);
CREATE INDEX IF NOT EXISTS idx_user_branches_branch ON user_branches(branch_id);

-- Backfill user_branches from existing users.branch_id
INSERT INTO user_branches (user_id, branch_id, is_primary)
SELECT u.id, u.branch_id, TRUE
FROM users u
WHERE u.branch_id IS NOT NULL
ON CONFLICT (user_id, branch_id) DO UPDATE SET is_primary = TRUE;

-- 6. Extend audit_logs with resource tagging
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS resource_type VARCHAR(50);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS resource_id VARCHAR(50);
CREATE INDEX IF NOT EXISTS idx_audit_logs_resource ON audit_logs(resource_type, resource_id);

-- 7. Seed standard roles and permission sets for each business
DO $$
DECLARE
    b RECORD;
    v_role_id VARCHAR(36);
BEGIN
    FOR b IN SELECT id FROM businesses LOOP
        -- Business Owner (Full Access)
        SELECT id INTO v_role_id FROM access_roles WHERE business_id = b.id AND LOWER(name) IN ('business owner', 'owner') LIMIT 1;
        IF v_role_id IS NULL THEN
            v_role_id := gen_random_uuid()::text;
            INSERT INTO access_roles (id, business_id, name, description, allowed_menus, is_active, created_at, updated_at)
            VALUES (v_role_id, b.id, 'Business Owner', 'Full access to business operations and settings',
                    'DASHBOARD,POS,HOSPITALITY,HOSPITALITY_OPS,SERVICES,OPEN_TABS,INVENTORY,PURCHASES,ORDERS,CUSTOMERS,EXPENSES,PAYMENTS,CARD_PAYMENTS,TAX,KRA,SOCIAL,SOCIAL_SETUP,USERS,AUDIT_LOG,REPORTS,DOWNLOADS,SETTINGS',
                    TRUE, NOW(), NOW());
        END IF;
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT v_role_id, p.id FROM permissions p
        ON CONFLICT DO NOTHING;

        -- Business Admin (Users, Outlets, Products, Orders, Config)
        SELECT id INTO v_role_id FROM access_roles WHERE business_id = b.id AND LOWER(name) IN ('business admin', 'admin') LIMIT 1;
        IF v_role_id IS NULL THEN
            v_role_id := gen_random_uuid()::text;
            INSERT INTO access_roles (id, business_id, name, description, allowed_menus, is_active, created_at, updated_at)
            VALUES (v_role_id, b.id, 'Business Admin', 'Manage users, outlets, products, orders and configurations',
                    'DASHBOARD,POS,HOSPITALITY,SERVICES,OPEN_TABS,INVENTORY,PURCHASES,ORDERS,CUSTOMERS,EXPENSES,PAYMENTS,CARD_PAYMENTS,TAX,KRA,USERS,AUDIT_LOG,REPORTS,SETTINGS',
                    TRUE, NOW(), NOW());
        END IF;
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT v_role_id, p.id FROM permissions p
        WHERE p.code NOT IN ('payments.refund')
        ON CONFLICT DO NOTHING;

        -- Manager (Assigned outlets, orders, inventory, reports)
        SELECT id INTO v_role_id FROM access_roles WHERE business_id = b.id AND LOWER(name) = 'manager' LIMIT 1;
        IF v_role_id IS NULL THEN
            v_role_id := gen_random_uuid()::text;
            INSERT INTO access_roles (id, business_id, name, description, allowed_menus, is_active, created_at, updated_at)
            VALUES (v_role_id, b.id, 'Manager', 'Manage assigned outlets, orders, inventory and reports',
                    'DASHBOARD,POS,HOSPITALITY,SERVICES,OPEN_TABS,INVENTORY,ORDERS,CUSTOMERS,EXPENSES,PAYMENTS,REPORTS',
                    TRUE, NOW(), NOW());
        END IF;
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT v_role_id, p.id FROM permissions p
        WHERE p.code IN (
            'users.view', 'users.unlock', 'users.reset_pin',
            'orders.view', 'orders.create', 'orders.update', 'orders.cancel', 'orders.refund',
            'products.view', 'products.create', 'products.update',
            'inventory.view', 'inventory.adjust',
            'payments.view', 'payments.refund',
            'reports.sales', 'reports.inventory'
        )
        ON CONFLICT DO NOTHING;

        -- Cashier (POS, payments, orders, receipts)
        SELECT id INTO v_role_id FROM access_roles WHERE business_id = b.id AND LOWER(name) IN ('cashier', 'front office / cashier') LIMIT 1;
        IF v_role_id IS NULL THEN
            v_role_id := gen_random_uuid()::text;
            INSERT INTO access_roles (id, business_id, name, description, allowed_menus, is_active, created_at, updated_at)
            VALUES (v_role_id, b.id, 'Cashier', 'POS, payments, orders and customer checkout',
                    'POS,OPEN_TABS,PAYMENTS,CARD_PAYMENTS,ORDERS,CUSTOMERS',
                    TRUE, NOW(), NOW());
        END IF;
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT v_role_id, p.id FROM permissions p
        WHERE p.code IN ('orders.view', 'orders.create', 'orders.update', 'products.view', 'payments.view')
        ON CONFLICT DO NOTHING;

        -- Kitchen/Order Staff (View & process assigned orders)
        SELECT id INTO v_role_id FROM access_roles WHERE business_id = b.id AND LOWER(name) IN ('kitchen staff', 'order staff', 'kitchen/order staff') LIMIT 1;
        IF v_role_id IS NULL THEN
            v_role_id := gen_random_uuid()::text;
            INSERT INTO access_roles (id, business_id, name, description, allowed_menus, is_active, created_at, updated_at)
            VALUES (v_role_id, b.id, 'Kitchen/Order Staff', 'View and process kitchen tickets and order status',
                    'ORDERS,HOSPITALITY_OPS',
                    TRUE, NOW(), NOW());
        END IF;
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT v_role_id, p.id FROM permissions p
        WHERE p.code IN ('orders.view', 'orders.update', 'products.view')
        ON CONFLICT DO NOTHING;

        -- Inventory Staff (Products, stock movements and inventory)
        SELECT id INTO v_role_id FROM access_roles WHERE business_id = b.id AND LOWER(name) IN ('inventory staff', 'storekeeper') LIMIT 1;
        IF v_role_id IS NULL THEN
            v_role_id := gen_random_uuid()::text;
            INSERT INTO access_roles (id, business_id, name, description, allowed_menus, is_active, created_at, updated_at)
            VALUES (v_role_id, b.id, 'Inventory Staff', 'Products, stock movements, and inventory adjustments',
                    'INVENTORY,PURCHASES,EXPENSES',
                    TRUE, NOW(), NOW());
        END IF;
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT v_role_id, p.id FROM permissions p
        WHERE p.code IN ('products.view', 'products.create', 'products.update', 'inventory.view', 'inventory.adjust', 'reports.inventory')
        ON CONFLICT DO NOTHING;

        -- Accountant (Sales, settlements, expenses, financial reports)
        SELECT id INTO v_role_id FROM access_roles WHERE business_id = b.id AND LOWER(name) = 'accountant' LIMIT 1;
        IF v_role_id IS NULL THEN
            v_role_id := gen_random_uuid()::text;
            INSERT INTO access_roles (id, business_id, name, description, allowed_menus, is_active, created_at, updated_at)
            VALUES (v_role_id, b.id, 'Accountant', 'Sales, settlements, expenses and financial reports',
                    'DASHBOARD,EXPENSES,PAYMENTS,TAX,KRA,REPORTS',
                    TRUE, NOW(), NOW());
        END IF;
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT v_role_id, p.id FROM permissions p
        WHERE p.code IN ('orders.view', 'payments.view', 'reports.sales', 'reports.financial', 'reports.inventory', 'inventory.view')
        ON CONFLICT DO NOTHING;

        -- Viewer/Auditor (Read-only access)
        SELECT id INTO v_role_id FROM access_roles WHERE business_id = b.id AND LOWER(name) IN ('viewer', 'auditor', 'viewer/auditor') LIMIT 1;
        IF v_role_id IS NULL THEN
            v_role_id := gen_random_uuid()::text;
            INSERT INTO access_roles (id, business_id, name, description, allowed_menus, is_active, created_at, updated_at)
            VALUES (v_role_id, b.id, 'Viewer/Auditor', 'Read-only visibility across reports, orders and logs',
                    'DASHBOARD,ORDERS,PAYMENTS,AUDIT_LOG,REPORTS',
                    TRUE, NOW(), NOW());
        END IF;
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT v_role_id, p.id FROM permissions p
        WHERE p.action = 'VIEW'
        ON CONFLICT DO NOTHING;

    END LOOP;
END $$;
