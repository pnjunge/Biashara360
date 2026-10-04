-- Cashier presets retain checkout and kitchen workflow, not stock administration.
UPDATE access_roles SET allowed_menus=COALESCE((SELECT string_agg(TRIM(menu),',') FROM unnest(string_to_array(allowed_menus,',')) menu WHERE UPPER(TRIM(menu)) NOT IN ('INVENTORY','PURCHASES','HOSPITALITY_OPS')),''),updated_at=NOW()
WHERE LOWER(TRIM(name))='cashier';
UPDATE access_groups SET allowed_menus=COALESCE((SELECT string_agg(TRIM(menu),',') FROM unnest(string_to_array(allowed_menus,',')) menu WHERE UPPER(TRIM(menu)) NOT IN ('INVENTORY','PURCHASES','HOSPITALITY_OPS')),''),updated_at=NOW()
WHERE LOWER(TRIM(name))='cashier';
DELETE FROM role_permissions rp USING access_roles r,permissions p WHERE rp.role_id=r.id AND rp.permission_id=p.id AND LOWER(TRIM(r.name))='cashier'
AND (p.code IN ('products.create','products.update','products.delete','inventory.adjust','inventory.transfer','inventory.suppliers','hospitality.menu','hospitality.stock','hospitality.purchasing','hospitality.approvals','hospitality.shifts','hospitality.reports','hospitality.floor','hospitality.reservations') OR p.module='PURCHASES');
INSERT INTO permissions(id,code,module,action,name,description,created_at) VALUES
('perm_purchases_view','purchases.view','PURCHASES','VIEW','View purchases','View supplier purchase invoices',NOW()),
('perm_purchases_create','purchases.create','PURCHASES','CREATE','Create purchases','Record purchase invoices and receive product stock',NOW()),
('perm_inventory_suppliers','inventory.suppliers','INVENTORY','MANAGE','Manage suppliers','View and maintain inventory suppliers',NOW()) ON CONFLICT(code) DO NOTHING;
-- Preserve purchasing for existing stock-management roles; cashier roles were trimmed above.
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM access_roles r CROSS JOIN permissions p
WHERE LOWER(TRIM(r.name))<>'cashier' AND p.code IN ('purchases.view','purchases.create','inventory.suppliers')
AND (LOWER(TRIM(r.name)) IN ('business owner','business admin','admin','manager','storekeeper','inventory staff','inventory clerk') OR EXISTS(SELECT 1 FROM unnest(string_to_array(r.allowed_menus,',')) m WHERE UPPER(TRIM(m)) IN ('INVENTORY','PURCHASES')))
ON CONFLICT DO NOTHING;
-- Inventory staff used the inventory screen's purchasing tab before explicit purchase menus.
UPDATE access_roles SET allowed_menus=allowed_menus||',PURCHASES',updated_at=NOW()
WHERE LOWER(TRIM(name))<>'cashier' AND EXISTS(SELECT 1 FROM unnest(string_to_array(allowed_menus,',')) m WHERE UPPER(TRIM(m))='INVENTORY')
AND NOT EXISTS(SELECT 1 FROM unnest(string_to_array(allowed_menus,',')) m WHERE UPPER(TRIM(m))='PURCHASES');
UPDATE access_groups SET allowed_menus=allowed_menus||',PURCHASES',updated_at=NOW()
WHERE LOWER(TRIM(name))<>'cashier' AND EXISTS(SELECT 1 FROM unnest(string_to_array(allowed_menus,',')) m WHERE UPPER(TRIM(m))='INVENTORY')
AND NOT EXISTS(SELECT 1 FROM unnest(string_to_array(allowed_menus,',')) m WHERE UPPER(TRIM(m))='PURCHASES');
UPDATE businesses SET enabled_menus=enabled_menus||',PURCHASES'
WHERE EXISTS(SELECT 1 FROM unnest(string_to_array(enabled_menus,',')) m WHERE UPPER(TRIM(m))='INVENTORY')
AND NOT EXISTS(SELECT 1 FROM unnest(string_to_array(enabled_menus,',')) m WHERE UPPER(TRIM(m))='PURCHASES');
ALTER TABLE businesses ALTER COLUMN enabled_menus SET DEFAULT 'DASHBOARD,POS,HOSPITALITY,HOSPITALITY_OPS,SERVICES,OPEN_TABS,INVENTORY,PURCHASES,ORDERS,CUSTOMERS,EXPENSES,PAYMENTS,CARD_PAYMENTS,TAX,KRA,SOCIAL,SOCIAL_SETUP,USERS,REPORTS,DOWNLOADS,SETTINGS';
