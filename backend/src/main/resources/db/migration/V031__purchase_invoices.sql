CREATE TABLE IF NOT EXISTS purchase_invoices (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    invoice_number VARCHAR(100) NOT NULL,
    supplier_name VARCHAR(255) NOT NULL,
    supplier_phone VARCHAR(30),
    total_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PAID',
    payment_method VARCHAR(30) NOT NULL DEFAULT 'CASH',
    notes TEXT NOT NULL DEFAULT '',
    items_json TEXT NOT NULL DEFAULT '[]',
    invoice_date TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_purchase_invoices_biz_date ON purchase_invoices(business_id, invoice_date DESC);
CREATE INDEX IF NOT EXISTS idx_purchase_invoices_inv_num ON purchase_invoices(business_id, invoice_number);

UPDATE businesses
SET enabled_menus = CASE
    WHEN enabled_menus = '' THEN 'PURCHASES'
    WHEN POSITION('PURCHASES' IN enabled_menus) = 0 THEN enabled_menus || ',PURCHASES'
    ELSE enabled_menus
END;
