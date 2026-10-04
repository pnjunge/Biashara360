-- V040: Promotions & Coupons Engine, Product Units of Measure (UOM), Custom Domain CNAME Mapping, and Double-Entry General Ledger

-- 1. Coupon Codes Table
CREATE TABLE IF NOT EXISTS coupon_codes (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    code VARCHAR(32) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    discount_type VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE', -- PERCENTAGE | FIXED_AMOUNT
    discount_value DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    minimum_order_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    max_discount_amount DOUBLE PRECISION,
    usage_limit INTEGER,
    usage_count INTEGER NOT NULL DEFAULT 0,
    start_date TIMESTAMPTZ,
    end_date TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_coupon_business_code UNIQUE (business_id, code)
);

CREATE INDEX IF NOT EXISTS idx_coupon_codes_biz ON coupon_codes(business_id, is_active);

-- 2. Automated Promotion Rules Table
CREATE TABLE IF NOT EXISTS promotion_rules (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(30) NOT NULL, -- BUY_X_GET_Y_FREE | TIERED_SPEND | BUNDLE_DEAL
    trigger_product_id VARCHAR(36) REFERENCES products(id) ON DELETE CASCADE,
    trigger_quantity INTEGER NOT NULL DEFAULT 1,
    reward_product_id VARCHAR(36) REFERENCES products(id) ON DELETE SET NULL,
    reward_quantity INTEGER NOT NULL DEFAULT 1,
    discount_percent DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    minimum_spend DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_promotion_rules_biz ON promotion_rules(business_id, is_active);

-- 3. Product Base Unit & UOM Conversions
ALTER TABLE products ADD COLUMN IF NOT EXISTS base_unit VARCHAR(30) NOT NULL DEFAULT 'PCS';

CREATE TABLE IF NOT EXISTS product_uom_conversions (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    product_id VARCHAR(36) NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    unit_name VARCHAR(50) NOT NULL, -- e.g. "Carton", "Box", "Dozen", "Bale", "Crate"
    conversion_factor DOUBLE PRECISION NOT NULL DEFAULT 1.0, -- e.g. 24.0 means 1 carton = 24 base units
    selling_price DOUBLE PRECISION NOT NULL,
    buying_price DOUBLE PRECISION,
    barcode VARCHAR(100),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_uom_conversions_product ON product_uom_conversions(product_id);
CREATE INDEX IF NOT EXISTS idx_uom_conversions_biz ON product_uom_conversions(business_id);

-- 4. Custom Domain CNAME Mapping for Businesses
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS custom_domain VARCHAR(255) UNIQUE;
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS custom_domain_status VARCHAR(30) NOT NULL DEFAULT 'UNCONFIGURED';
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS custom_domain_cname_target VARCHAR(255) NOT NULL DEFAULT 'ingress.biashara360.co.ke';
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS custom_domain_verified_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_businesses_custom_domain ON businesses(custom_domain);

-- 5. Double-Entry General Ledger: Chart of Accounts & Journal Entries
CREATE TABLE IF NOT EXISTS chart_of_accounts (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    account_code VARCHAR(20) NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    account_type VARCHAR(20) NOT NULL, -- ASSET | LIABILITY | EQUITY | REVENUE | EXPENSE
    normal_balance VARCHAR(10) NOT NULL DEFAULT 'DEBIT', -- DEBIT | CREDIT
    current_balance DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_chart_of_accounts_code UNIQUE (business_id, account_code)
);

CREATE INDEX IF NOT EXISTS idx_chart_of_accounts_biz ON chart_of_accounts(business_id, account_type);

CREATE TABLE IF NOT EXISTS journal_entries (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    entry_number VARCHAR(30) NOT NULL,
    entry_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    narration TEXT NOT NULL,
    source_module VARCHAR(30) NOT NULL DEFAULT 'MANUAL', -- MANUAL | POS | PURCHASES | EXPENSES | TAX
    source_reference_id VARCHAR(64),
    total_debit DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_credit DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    is_balanced BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_journal_entries_biz_date ON journal_entries(business_id, entry_date DESC);

CREATE TABLE IF NOT EXISTS journal_entry_lines (
    id VARCHAR(36) PRIMARY KEY,
    entry_id VARCHAR(36) NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    account_id VARCHAR(36) NOT NULL REFERENCES chart_of_accounts(id),
    description VARCHAR(255) NOT NULL DEFAULT '',
    debit DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    credit DOUBLE PRECISION NOT NULL DEFAULT 0.0
);

CREATE INDEX IF NOT EXISTS idx_journal_lines_entry ON journal_entry_lines(entry_id);
CREATE INDEX IF NOT EXISTS idx_journal_lines_account ON journal_entry_lines(account_id);

-- Update enabled menus to include Accounting
UPDATE businesses
SET enabled_menus = CASE
    WHEN enabled_menus = '' THEN 'ACCOUNTING'
    WHEN POSITION('ACCOUNTING' IN enabled_menus) = 0 THEN enabled_menus || ',ACCOUNTING'
    ELSE enabled_menus
END;

-- 6. Fix chk_orders_tab_status constraint to support AWAITING_PAYMENT
ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_tab_status;
ALTER TABLE orders ADD CONSTRAINT chk_orders_tab_status
    CHECK (tab_status IN ('OPEN', 'CLOSED', 'MERGED', 'SPLIT', 'AWAITING_PAYMENT'));

-- 7. Platform Operating Expenses (Super Admin Platform Management)
CREATE TABLE IF NOT EXISTS platform_expenses (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL, -- INFRASTRUCTURE | API_FEES | SMS_GATEWAY | DOMAIN_SSL | SUPPORT | GENERAL
    amount DOUBLE PRECISION NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'KES',
    vendor VARCHAR(100) NOT NULL DEFAULT '',
    expense_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    notes TEXT NOT NULL DEFAULT '',
    created_by VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_platform_expenses_date ON platform_expenses(expense_date DESC);
CREATE INDEX IF NOT EXISTS idx_platform_expenses_cat ON platform_expenses(category);

-- Seed initial platform operating baseline expenses
INSERT INTO platform_expenses (id, title, category, amount, currency, vendor, expense_date, notes) VALUES
    ('pexp-infra-01', 'Cloud Infrastructure & DB Hosting', 'INFRASTRUCTURE', 12500.0, 'KES', 'Railway / AWS', NOW() - INTERVAL '10 days', 'Platform production compute and managed PostgreSQL instance'),
    ('pexp-daraja-01', 'Safaricom Daraja API Gateway', 'API_FEES', 3500.0, 'KES', 'Safaricom Daraja', NOW() - INTERVAL '7 days', 'B2C and STK push processing operational charges'),
    ('pexp-sms-01', 'SMS Gateway Airtime & OTP Delivery', 'SMS_GATEWAY', 4800.0, 'KES', 'Africas Talking', NOW() - INTERVAL '4 days', 'Merchant authentication & transactional notifications bundle'),
    ('pexp-ssl-01', 'Wildcard SSL & Cloudflare DNS Protection', 'DOMAIN_SSL', 2200.0, 'KES', 'Cloudflare', NOW() - INTERVAL '2 days', 'Enterprise SSL termination & DDoS edge protection')
ON CONFLICT (id) DO NOTHING;
