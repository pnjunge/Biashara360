-- V036: Assign permissions/rights directly to access_groups, and assign users directly to groups
-- Eliminates the confusing intermediate roles layer (e.g. "Front · Roles: test")

-- 1. Add allowed_menus directly to access_groups
ALTER TABLE access_groups ADD COLUMN IF NOT EXISTS allowed_menus TEXT NOT NULL DEFAULT '';

-- 2. Populate allowed_menus for existing groups by aggregating distinct allowed_menus from linked roles
UPDATE access_groups g
SET allowed_menus = sub.combined_menus
FROM (
    SELECT agr.group_id, string_agg(DISTINCT ar.allowed_menus, ',') AS combined_menus
    FROM access_group_roles agr
    JOIN access_roles ar ON agr.role_id = ar.id
    WHERE ar.allowed_menus IS NOT NULL AND TRIM(ar.allowed_menus) <> ''
    GROUP BY agr.group_id
) sub
WHERE g.id = sub.group_id AND (g.allowed_menus IS NULL OR TRIM(g.allowed_menus) = '');

-- 3. If "Front" group exists, give it descriptive name and appropriate operational rights
UPDATE access_groups
SET name = 'Front Office / Cashier',
    description = 'Front counter operations, POS, open tabs and customer payments',
    allowed_menus = 'POS,OPEN_TABS,PAYMENTS,CARD_PAYMENTS,ORDERS,CUSTOMERS'
WHERE LOWER(name) = 'front';

-- 4. Set fallback permissions for any remaining groups with empty allowed_menus
UPDATE access_groups
SET allowed_menus = CASE
    WHEN LOWER(name) LIKE '%cashier%' THEN 'POS,OPEN_TABS,PAYMENTS,CARD_PAYMENTS,ORDERS,CUSTOMERS'
    WHEN LOWER(name) LIKE '%supervisor%' THEN 'HOSPITALITY,OPEN_TABS,HOSPITALITY_OPS,ORDERS,REPORTS'
    WHEN LOWER(name) LIKE '%inventory%' OR LOWER(name) LIKE '%store%' THEN 'INVENTORY,PURCHASES,EXPENSES'
    WHEN LOWER(name) LIKE '%manager%' THEN 'DASHBOARD,POS,HOSPITALITY,HOSPITALITY_OPS,SERVICES,OPEN_TABS,INVENTORY,PURCHASES,ORDERS,CUSTOMERS,EXPENSES,PAYMENTS,CARD_PAYMENTS,TAX,KRA,SOCIAL,SOCIAL_SETUP,USERS,AUDIT_LOG,REPORTS,DOWNLOADS,SETTINGS'
    ELSE 'POS,OPEN_TABS,PAYMENTS,ORDERS,CUSTOMERS'
END
WHERE allowed_menus IS NULL OR TRIM(allowed_menus) = '';

-- 5. Seed standard operational groups for businesses that don't have them yet
DO $$
DECLARE
    b RECORD;
BEGIN
    FOR b IN SELECT id FROM businesses LOOP
        -- Cashier
        IF NOT EXISTS (SELECT 1 FROM access_groups WHERE business_id = b.id AND LOWER(name) IN ('cashier', 'front office / cashier')) THEN
            INSERT INTO access_groups (id, business_id, name, description, allowed_menus, is_active, created_at, updated_at)
            VALUES (gen_random_uuid()::text, b.id, 'Cashier', 'Point of sale, open tabs, collections and customer sales', 'POS,OPEN_TABS,PAYMENTS,CARD_PAYMENTS,ORDERS,CUSTOMERS', TRUE, NOW(), NOW());
        END IF;

        -- Supervisor
        IF NOT EXISTS (SELECT 1 FROM access_groups WHERE business_id = b.id AND LOWER(name) = 'supervisor') THEN
            INSERT INTO access_groups (id, business_id, name, description, allowed_menus, is_active, created_at, updated_at)
            VALUES (gen_random_uuid()::text, b.id, 'Supervisor', 'Floor operations, bar/restaurant supervision, orders and reports', 'HOSPITALITY,OPEN_TABS,HOSPITALITY_OPS,ORDERS,REPORTS', TRUE, NOW(), NOW());
        END IF;

        -- Storekeeper
        IF NOT EXISTS (SELECT 1 FROM access_groups WHERE business_id = b.id AND LOWER(name) IN ('storekeeper', 'inventory clerk')) THEN
            INSERT INTO access_groups (id, business_id, name, description, allowed_menus, is_active, created_at, updated_at)
            VALUES (gen_random_uuid()::text, b.id, 'Storekeeper', 'Inventory stock tracking, purchases, and operating expenses', 'INVENTORY,PURCHASES,EXPENSES', TRUE, NOW(), NOW());
        END IF;

        -- Manager
        IF NOT EXISTS (SELECT 1 FROM access_groups WHERE business_id = b.id AND LOWER(name) = 'manager') THEN
            INSERT INTO access_groups (id, business_id, name, description, allowed_menus, is_active, created_at, updated_at)
            VALUES (gen_random_uuid()::text, b.id, 'Manager', 'Full business operations, reports, accounting and store settings', 'DASHBOARD,POS,HOSPITALITY,HOSPITALITY_OPS,SERVICES,OPEN_TABS,INVENTORY,PURCHASES,ORDERS,CUSTOMERS,EXPENSES,PAYMENTS,CARD_PAYMENTS,TAX,KRA,REPORTS,DOWNLOADS,SETTINGS', TRUE, NOW(), NOW());
        END IF;
    END LOOP;
END $$;

-- 6. Migrate any user assigned via user_access_roles into user_access_groups
DO $$
DECLARE
    r RECORD;
    target_gid VARCHAR(36);
BEGIN
    FOR r IN
        SELECT DISTINCT uar.user_id, u.business_id, ar.name AS role_name, ar.allowed_menus
        FROM user_access_roles uar
        JOIN users u ON uar.user_id = u.id
        JOIN access_roles ar ON uar.role_id = ar.id
        WHERE u.business_id IS NOT NULL
    LOOP
        -- Find existing group matching role name or create one
        SELECT id INTO target_gid FROM access_groups WHERE business_id = r.business_id AND LOWER(name) = LOWER(r.role_name) LIMIT 1;
        IF target_gid IS NULL THEN
            target_gid := gen_random_uuid()::text;
            INSERT INTO access_groups (id, business_id, name, description, allowed_menus, is_active, created_at, updated_at)
            VALUES (target_gid, r.business_id, r.role_name, 'Migrated permission group', r.allowed_menus, TRUE, NOW(), NOW());
        END IF;

        INSERT INTO user_access_groups (user_id, group_id)
        VALUES (r.user_id, target_gid)
        ON CONFLICT DO NOTHING;
    END LOOP;
END $$;
