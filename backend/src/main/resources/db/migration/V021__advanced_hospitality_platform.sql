ALTER TABLE hospitality_tables ADD COLUMN waiter_user_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE hospitality_tables ADD COLUMN merged_into_table_id VARCHAR(36) REFERENCES hospitality_tables(id) ON DELETE SET NULL;
ALTER TABLE hospitality_tables ADD COLUMN position_x INTEGER NOT NULL DEFAULT 0;
ALTER TABLE hospitality_tables ADD COLUMN position_y INTEGER NOT NULL DEFAULT 0;
ALTER TABLE hospitality_tables ADD COLUMN shape VARCHAR(20) NOT NULL DEFAULT 'RECTANGLE';
ALTER TABLE order_items ADD COLUMN modifiers_json TEXT NOT NULL DEFAULT '[]';
ALTER TABLE order_items ADD COLUMN item_note VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE order_items ADD COLUMN discount_amount DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE order_items ADD COLUMN complimentary BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE order_split_payments (
    id VARCHAR(36) PRIMARY KEY, business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    order_id VARCHAR(36) NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    amount DOUBLE PRECISION NOT NULL CHECK (amount > 0), method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', transaction_code VARCHAR(100),
    created_by VARCHAR(36) REFERENCES users(id), created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE hospitality_reservations (
    id VARCHAR(36) PRIMARY KEY, business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    table_id VARCHAR(36) REFERENCES hospitality_tables(id) ON DELETE SET NULL,
    customer_name VARCHAR(255) NOT NULL, customer_phone VARCHAR(20) NOT NULL DEFAULT '', guest_count INTEGER NOT NULL,
    reserved_at TIMESTAMPTZ NOT NULL, duration_minutes INTEGER NOT NULL DEFAULT 90,
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED', notes VARCHAR(500) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE hospitality_menu_profiles (
    product_id VARCHAR(36) PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    preparation_station VARCHAR(20), meal_periods TEXT NOT NULL DEFAULT '',
    sizes_json TEXT NOT NULL DEFAULT '[]', extras_json TEXT NOT NULL DEFAULT '[]', variants_json TEXT NOT NULL DEFAULT '[]',
    combo_json TEXT NOT NULL DEFAULT '[]', sold_out BOOLEAN NOT NULL DEFAULT FALSE,
    happy_hour_price DOUBLE PRECISION, happy_hour_start VARCHAR(5), happy_hour_end VARCHAR(5),
    age_restricted BOOLEAN NOT NULL DEFAULT FALSE, minimum_age INTEGER,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE inventory_ingredients (
    id VARCHAR(36) PRIMARY KEY, business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL, unit VARCHAR(20) NOT NULL, quantity DOUBLE PRECISION NOT NULL DEFAULT 0,
    reorder_level DOUBLE PRECISION NOT NULL DEFAULT 0, unit_cost DOUBLE PRECISION NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_inventory_ingredient_name UNIQUE (business_id, name)
);

CREATE TABLE product_recipes (
    product_id VARCHAR(36) NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    ingredient_id VARCHAR(36) NOT NULL REFERENCES inventory_ingredients(id) ON DELETE CASCADE,
    quantity DOUBLE PRECISION NOT NULL CHECK (quantity > 0), PRIMARY KEY (product_id, ingredient_id)
);

CREATE TABLE bar_stock_events (
    id VARCHAR(36) PRIMARY KEY, business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    product_id VARCHAR(36) REFERENCES products(id) ON DELETE SET NULL,
    ingredient_id VARCHAR(36) REFERENCES inventory_ingredients(id) ON DELETE SET NULL,
    event_type VARCHAR(30) NOT NULL, quantity DOUBLE PRECISION NOT NULL, unit VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NOT NULL DEFAULT '', recorded_by VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    recorded_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE hospitality_shifts (
    id VARCHAR(36) PRIMARY KEY, business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    opened_by VARCHAR(36) NOT NULL REFERENCES users(id), closed_by VARCHAR(36) REFERENCES users(id),
    opened_at TIMESTAMPTZ NOT NULL, closed_at TIMESTAMPTZ,
    opening_float DOUBLE PRECISION NOT NULL DEFAULT 0, expected_cash DOUBLE PRECISION,
    actual_cash DOUBLE PRECISION, mpesa_total DOUBLE PRECISION, card_total DOUBLE PRECISION,
    tips_total DOUBLE PRECISION NOT NULL DEFAULT 0, expenses_total DOUBLE PRECISION NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN', notes VARCHAR(500) NOT NULL DEFAULT ''
);

CREATE TABLE suppliers (
    id VARCHAR(36) PRIMARY KEY, business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL, phone VARCHAR(20) NOT NULL DEFAULT '', email VARCHAR(255), address VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE purchase_orders (
    id VARCHAR(36) PRIMARY KEY, business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    supplier_id VARCHAR(36) NOT NULL REFERENCES suppliers(id), order_number VARCHAR(60) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', ordered_at TIMESTAMPTZ NOT NULL, received_at TIMESTAMPTZ,
    total_cost DOUBLE PRECISION NOT NULL DEFAULT 0, notes VARCHAR(500) NOT NULL DEFAULT '', created_by VARCHAR(36) REFERENCES users(id),
    CONSTRAINT uq_purchase_order_number UNIQUE (business_id, order_number)
);

CREATE TABLE purchase_order_items (
    id VARCHAR(36) PRIMARY KEY, purchase_order_id VARCHAR(36) NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    ingredient_id VARCHAR(36) NOT NULL REFERENCES inventory_ingredients(id), ordered_quantity DOUBLE PRECISION NOT NULL,
    received_quantity DOUBLE PRECISION NOT NULL DEFAULT 0, unit_cost DOUBLE PRECISION NOT NULL
);

CREATE TABLE manager_approvals (
    id VARCHAR(36) PRIMARY KEY, business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    action_type VARCHAR(30) NOT NULL, entity_type VARCHAR(30) NOT NULL, entity_id VARCHAR(36) NOT NULL,
    requested_by VARCHAR(36) NOT NULL REFERENCES users(id), approved_by VARCHAR(36) REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', reason VARCHAR(500) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL, decided_at TIMESTAMPTZ
);

CREATE TABLE audit_events (
    id VARCHAR(36) PRIMARY KEY, business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    user_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL, action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(40) NOT NULL, entity_id VARCHAR(36), details TEXT NOT NULL DEFAULT '{}',
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_hospitality_reservations_schedule ON hospitality_reservations(business_id, reserved_at, status);
CREATE INDEX idx_inventory_ingredients_low_stock ON inventory_ingredients(business_id, is_active, quantity);
CREATE INDEX idx_bar_stock_events_product_date ON bar_stock_events(business_id, product_id, recorded_at DESC);
CREATE UNIQUE INDEX uq_open_hospitality_shift ON hospitality_shifts(business_id) WHERE status = 'OPEN';
CREATE INDEX idx_audit_events_business_date ON audit_events(business_id, occurred_at DESC);

UPDATE businesses SET enabled_menus = enabled_menus || ',HOSPITALITY_OPS'
WHERE POSITION('HOSPITALITY_OPS' IN enabled_menus) = 0;

INSERT INTO access_roles(id,business_id,name,description,allowed_menus,is_active,created_at,updated_at)
SELECT SUBSTRING(hash,1,8)||'-'||SUBSTRING(hash,9,4)||'-'||SUBSTRING(hash,13,4)||'-'||SUBSTRING(hash,17,4)||'-'||SUBSTRING(hash,21,12),business_id,role_name,description,menus,TRUE,NOW(),NOW()
FROM (SELECT b.id business_id,r.role_name,r.description,r.menus,MD5(b.id||':hospitality-role:'||r.role_name) hash FROM businesses b CROSS JOIN (VALUES
 ('Waiter','Table service and customer tabs','POS,HOSPITALITY,OPEN_TABS'),('Bartender','Bar tickets and stock','HOSPITALITY,OPEN_TABS,HOSPITALITY_OPS,INVENTORY'),
 ('Cashier','Settlement and reconciliation','POS,OPEN_TABS,PAYMENTS'),('Chef','Kitchen display workflow','HOSPITALITY'),
 ('Supervisor','Hospitality supervision and reports','HOSPITALITY,OPEN_TABS,HOSPITALITY_OPS,REPORTS'),('Inventory Clerk','Stock and purchasing','INVENTORY,HOSPITALITY_OPS,EXPENSES'),
 ('Manager','Hospitality management and approvals','DASHBOARD,POS,HOSPITALITY,OPEN_TABS,HOSPITALITY_OPS,INVENTORY,ORDERS,CUSTOMERS,EXPENSES,PAYMENTS,REPORTS,SETTINGS')
) r(role_name,description,menus)) seeded ON CONFLICT DO NOTHING;
