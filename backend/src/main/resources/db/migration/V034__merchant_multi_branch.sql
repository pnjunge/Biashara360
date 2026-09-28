-- V034: Add capability for merchants to have multiple branches
-- Enables multi-branch management, branch-level user assignment, orders, expenses, and inventory tracking

-- 1. Create branches table
CREATE TABLE IF NOT EXISTS branches (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(255),
    address VARCHAR(500),
    city VARCHAR(100),
    county VARCHAR(100),
    is_head_office BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    receipt_header VARCHAR(255),
    receipt_footer VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for fast lookup
CREATE INDEX IF NOT EXISTS idx_branches_business_id ON branches (business_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_branches_business_code ON branches (business_id, code);

-- 2. Backfill default Main Branch for all existing businesses
INSERT INTO branches (
    id,
    business_id,
    name,
    code,
    phone,
    email,
    address,
    county,
    is_head_office,
    is_active,
    receipt_header,
    receipt_footer,
    created_at,
    updated_at
)
SELECT
    SUBSTRING(MD5(b.id || '_main_branch') FROM 1 FOR 36),
    b.id,
    CASE 
        WHEN b.name IS NOT NULL AND TRIM(b.name) <> '' THEN TRIM(b.name) || ' - Head Office'
        ELSE 'Head Office'
    END,
    'MAIN',
    b.owner_phone,
    b.owner_email,
    b.address,
    b.county,
    TRUE,
    TRUE,
    b.receipt_header,
    b.receipt_footer,
    NOW(),
    NOW()
FROM businesses b
WHERE NOT EXISTS (
    SELECT 1 FROM branches br WHERE br.business_id = b.id
);

-- 3. Add branch_id columns to related operational tables
ALTER TABLE users ADD COLUMN IF NOT EXISTS branch_id VARCHAR(36) REFERENCES branches(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_users_branch_id ON users(branch_id);

ALTER TABLE orders ADD COLUMN IF NOT EXISTS branch_id VARCHAR(36) REFERENCES branches(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_orders_branch_id ON orders(branch_id);

ALTER TABLE expenses ADD COLUMN IF NOT EXISTS branch_id VARCHAR(36) REFERENCES branches(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_expenses_branch_id ON expenses(branch_id);

ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS branch_id VARCHAR(36) REFERENCES branches(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_stock_movements_branch_id ON stock_movements(branch_id);

-- 4. Associate existing users, orders, expenses, and stock movements with their business's head office branch
UPDATE users u
SET branch_id = br.id
FROM branches br
WHERE u.business_id = br.business_id AND br.is_head_office = TRUE AND u.branch_id IS NULL;

UPDATE orders o
SET branch_id = br.id
FROM branches br
WHERE o.business_id = br.business_id AND br.is_head_office = TRUE AND o.branch_id IS NULL;

UPDATE expenses e
SET branch_id = br.id
FROM branches br
WHERE e.business_id = br.business_id AND br.is_head_office = TRUE AND e.branch_id IS NULL;

UPDATE stock_movements sm
SET branch_id = br.id
FROM branches br
WHERE sm.business_id = br.business_id AND br.is_head_office = TRUE AND sm.branch_id IS NULL;
