-- Cashiers can progress preparation tickets as part of the restaurant workflow.
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM access_roles r JOIN permissions p ON p.code='hospitality.kitchen'
WHERE LOWER(TRIM(r.name))='cashier' ON CONFLICT DO NOTHING;
UPDATE access_roles SET allowed_menus=allowed_menus||',HOSPITALITY',updated_at=NOW()
WHERE LOWER(TRIM(name))='cashier' AND POSITION(',HOSPITALITY,' IN ','||UPPER(REPLACE(allowed_menus,' ',''))||',')=0;
