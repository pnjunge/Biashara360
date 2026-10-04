ALTER TABLE purchase_orders ADD COLUMN payments_known BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE purchase_orders ALTER COLUMN payments_known SET DEFAULT TRUE;
CREATE TABLE ingredient_purchase_payments (
 id VARCHAR(36) PRIMARY KEY,
 business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
 purchase_order_id VARCHAR(36) NOT NULL REFERENCES purchase_orders(id),
 amount DOUBLE PRECISION NOT NULL CHECK(amount>0 AND amount<'Infinity'::DOUBLE PRECISION),
 method VARCHAR(20) NOT NULL CHECK(method IN ('CASH','MPESA','CARD','BANK_TRANSFER')),
 paid_from_till BOOLEAN NOT NULL DEFAULT FALSE,
 reference VARCHAR(120) NOT NULL DEFAULT '',
 client_reference VARCHAR(80) NOT NULL,
 recorded_by VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
 paid_at TIMESTAMPTZ NOT NULL,
 UNIQUE(business_id,client_reference)
);
CREATE UNIQUE INDEX ingredient_purchase_payment_reference ON ingredient_purchase_payments(business_id,method,reference) WHERE reference<>'';
CREATE INDEX ingredient_purchase_payment_date ON ingredient_purchase_payments(business_id,paid_at);
ALTER TABLE expenses ADD COLUMN purchase_order_id VARCHAR(36) REFERENCES purchase_orders(id);
CREATE UNIQUE INDEX expense_ingredient_purchase ON expenses(purchase_order_id) WHERE purchase_order_id IS NOT NULL;
-- Existing receipts gain traceable stock-purchase entries. Earlier payments remain unknown.
INSERT INTO expenses(id,business_id,category,amount,description,expense_date,recorded_at,purchase_order_id)
SELECT gen_random_uuid()::text,p.business_id,'STOCK_PURCHASE',p.total_cost,'Ingredient purchase: #'||p.order_number||' - '||s.name,
 (COALESCE(p.received_at,p.ordered_at) AT TIME ZONE 'Africa/Nairobi')::DATE,COALESCE(p.received_at,p.ordered_at),p.id
FROM purchase_orders p JOIN suppliers s ON s.id=p.supplier_id WHERE p.status='RECEIVED' AND p.total_cost>0;
INSERT INTO permissions(id,code,module,action,name,description,created_at)
VALUES('perm_hosp_purchase_payments','hospitality.purchase_payments','HOSPITALITY','MANAGE','Record supplier payments','Record ingredient supplier payments separately from receiving stock',NOW());
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM access_roles r CROSS JOIN permissions p WHERE LOWER(TRIM(r.name)) IN ('business owner','business admin','admin','manager','accountant') AND p.code='hospitality.purchase_payments' ON CONFLICT DO NOTHING;
