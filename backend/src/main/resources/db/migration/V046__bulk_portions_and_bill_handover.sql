ALTER TABLE inventory_ingredients ADD COLUMN purchase_unit VARCHAR(20);
ALTER TABLE inventory_ingredients ADD COLUMN purchase_unit_size DOUBLE PRECISION NOT NULL DEFAULT 1 CHECK (purchase_unit_size > 0);
ALTER TABLE purchase_order_items ADD COLUMN purchase_quantity DOUBLE PRECISION;
ALTER TABLE purchase_order_items ADD COLUMN purchase_unit VARCHAR(20);
ALTER TABLE purchase_order_items ADD COLUMN purchase_unit_cost DOUBLE PRECISION;
ALTER TABLE purchase_order_items ADD COLUMN conversion_factor DOUBLE PRECISION NOT NULL DEFAULT 1 CHECK (conversion_factor > 0);
ALTER TABLE order_items ADD COLUMN product_stock_deducted BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE orders ADD COLUMN responsible_user_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE orders ADD COLUMN settled_by_user_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE payments ADD COLUMN collected_by_user_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL;
UPDATE orders o SET responsible_user_id = o.server_user_id WHERE EXISTS (SELECT 1 FROM users u WHERE u.id = o.server_user_id AND u.business_id = o.business_id);
CREATE TABLE hospitality_staff_shifts (
 id VARCHAR(36) PRIMARY KEY, business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
 user_id VARCHAR(36) NOT NULL REFERENCES users(id), opened_at TIMESTAMPTZ NOT NULL,
 closed_at TIMESTAMPTZ, status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN','CLOSED')), notes VARCHAR(500) NOT NULL DEFAULT ''
);
CREATE UNIQUE INDEX idx_staff_shift_open ON hospitality_staff_shifts(business_id,user_id) WHERE status = 'OPEN';
CREATE TABLE hospitality_bill_handovers (
 id VARCHAR(36) PRIMARY KEY, business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
 order_id VARCHAR(36) NOT NULL REFERENCES orders(id), from_user_id VARCHAR(36) NOT NULL REFERENCES users(id),
 to_user_id VARCHAR(36) NOT NULL REFERENCES users(id), requested_by VARCHAR(36) NOT NULL REFERENCES users(id),
 status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','ACCEPTED','REJECTED','CANCELLED')),
 notes VARCHAR(500) NOT NULL DEFAULT '', balance_at_request DOUBLE PRECISION NOT NULL,
 requested_at TIMESTAMPTZ NOT NULL, decided_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_bill_handover_pending ON hospitality_bill_handovers(order_id) WHERE status = 'PENDING';
CREATE INDEX idx_bill_handover_business ON hospitality_bill_handovers(business_id,requested_at);
CREATE INDEX idx_orders_responsible ON orders(business_id,responsible_user_id);
ALTER TABLE orders ADD COLUMN completed_at TIMESTAMPTZ;
UPDATE orders o SET completed_at = COALESCE((SELECT MAX(p.transaction_date) FROM payments p WHERE p.order_id=o.id AND p.status='SUCCESS'),o.updated_at) WHERE o.payment_status='PAID';
ALTER TABLE hospitality_staff_shifts ADD COLUMN summary_json TEXT;
-- Drinks and other non-food lines no longer create preparation tickets.
UPDATE hospitality_menu_profiles SET preparation_station=NULL WHERE preparation_station='BAR';
UPDATE kitchen_tickets SET status='CANCELLED',updated_at=NOW() WHERE station='BAR' AND status NOT IN ('SERVED','CANCELLED');
ALTER TABLE order_items ADD COLUMN preparation_station VARCHAR(20);
UPDATE hospitality_menu_profiles m SET preparation_station=NULL FROM products p
WHERE p.id=m.product_id AND m.preparation_station='KITCHEN'
AND LOWER(TRIM(p.category)) !~ '(food|meal|dish|snack|bakery|breakfast|lunch|dinner|restaurant|kitchen|meat|beef|goat|chicken|fish|seafood|dessert|pastry|ugali|chapati|fries)';
UPDATE order_items i SET preparation_station='KITCHEN' FROM products p
WHERE p.id=i.product_id AND LOWER(TRIM(p.category)) ~ '(food|meal|dish|snack|bakery|breakfast|lunch|dinner|restaurant|kitchen|meat|beef|goat|chicken|fish|seafood|dessert|pastry|ugali|chapati|fries)'
AND EXISTS (SELECT 1 FROM kitchen_tickets t WHERE t.order_id=i.order_id AND t.station='KITCHEN' AND t.status<>'CANCELLED')
AND (NOT EXISTS (SELECT 1 FROM hospitality_menu_profiles m WHERE m.product_id=i.product_id) OR EXISTS (SELECT 1 FROM hospitality_menu_profiles m WHERE m.product_id=i.product_id AND m.preparation_station='KITCHEN'));
UPDATE kitchen_tickets t SET status='CANCELLED',updated_at=NOW()
WHERE t.station='KITCHEN' AND t.status NOT IN ('SERVED','CANCELLED')
AND NOT EXISTS (SELECT 1 FROM order_items i WHERE i.order_id=t.order_id AND i.preparation_station='KITCHEN');
