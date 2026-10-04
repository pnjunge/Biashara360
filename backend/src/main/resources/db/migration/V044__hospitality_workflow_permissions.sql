ALTER TABLE manager_approvals ADD COLUMN payload_json TEXT NOT NULL DEFAULT '{}';
INSERT INTO permissions(id,code,module,action,name,description,created_at)
SELECT 'perm_hosp_' || code, 'hospitality.' || code, 'HOSPITALITY', 'MANAGE', name, name, NOW()
FROM (VALUES ('view','View hospitality'),('orders','Take hospitality orders'),('kitchen','Update preparation tickets'),('billing','Settle hospitality tabs'),('reservations','Manage restaurant reservations'),('floor','Manage floor and tables'),('menu','Configure restaurant menus and recipes'),('stock','Manage ingredients and stock events'),('shifts','Open and close hospitality shifts'),('purchasing','Manage ingredient purchasing'),('reports','Read hospitality financial reports'),('approvals','Decide hospitality approvals')) AS p(code,name);
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM access_roles r CROSS JOIN permissions p
WHERE LOWER(r.name) IN ('business owner','business admin','admin','manager','supervisor') AND p.module='HOSPITALITY' ON CONFLICT DO NOTHING;
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM access_roles r CROSS JOIN permissions p WHERE
(LOWER(r.name)='cashier' AND p.code IN ('hospitality.view','hospitality.orders','hospitality.billing')) OR
(LOWER(r.name) IN ('kitchen/order staff','kitchen staff') AND p.code IN ('hospitality.view','hospitality.kitchen')) OR
(LOWER(r.name)='inventory clerk' AND p.code IN ('hospitality.view','hospitality.stock','hospitality.purchasing')) ON CONFLICT DO NOTHING;
UPDATE access_roles SET allowed_menus=REPLACE(allowed_menus,'HOSPITALITY_KITCHEN','HOSPITALITY') WHERE POSITION('HOSPITALITY_KITCHEN' IN allowed_menus)>0;
WITH layout AS (SELECT id,ROW_NUMBER() OVER (PARTITION BY business_id ORDER BY name,id)-1 AS n FROM hospitality_tables WHERE position_x=0 AND position_y=0)
UPDATE hospitality_tables t SET position_x=40+(layout.n%5)*150,position_y=40+(layout.n/5)*100 FROM layout WHERE t.id=layout.id;
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM access_roles r CROSS JOIN permissions p WHERE
(LOWER(r.name)='waiter' AND p.code IN ('hospitality.view','hospitality.orders','hospitality.reservations')) OR
(LOWER(r.name)='chef' AND p.code IN ('hospitality.view','hospitality.kitchen')) OR
(LOWER(r.name)='bartender' AND p.code IN ('hospitality.view','hospitality.orders','hospitality.kitchen','hospitality.stock')) ON CONFLICT DO NOTHING;
UPDATE orders SET tab_status='CLOSED' WHERE service_type IN ('DINE_IN','TAKEAWAY','DELIVERY') AND (payment_status IN ('CANCELLED','REFUNDED') OR delivery_status='CANCELLED');
UPDATE kitchen_tickets SET status='CANCELLED' WHERE order_id IN (SELECT id FROM orders WHERE payment_status IN ('CANCELLED','REFUNDED') OR delivery_status='CANCELLED');
UPDATE hospitality_tables t SET status='AVAILABLE' WHERE status='OCCUPIED' AND NOT EXISTS (SELECT 1 FROM orders o WHERE o.hospitality_table_id=t.id AND o.tab_status IN ('OPEN','AWAITING_PAYMENT'));
-- Approved complimentary hospitality tabs retain their items and can close with no payment.
ALTER TABLE orders DROP CONSTRAINT chk_orders_subtotal_positive;
ALTER TABLE orders ADD CONSTRAINT chk_orders_subtotal_positive CHECK (subtotal > 0 OR (subtotal = 0 AND service_type IN ('DINE_IN','TAKEAWAY','DELIVERY')));
ALTER TABLE orders DROP CONSTRAINT chk_orders_payment_method;
ALTER TABLE orders ADD CONSTRAINT chk_orders_payment_method CHECK (payment_method IN ('CASH','MPESA','CARD','BANK_TRANSFER','COD','CREDIT','TAB','SPLIT'));
UPDATE access_groups SET allowed_menus=REPLACE(allowed_menus,'HOSPITALITY_KITCHEN','HOSPITALITY') WHERE POSITION('HOSPITALITY_KITCHEN' IN allowed_menus)>0;
