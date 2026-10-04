ALTER TABLE businesses ADD COLUMN hotel_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_service_type;
ALTER TABLE orders ADD CONSTRAINT chk_orders_service_type CHECK (service_type IN ('RETAIL','DINE_IN','TAKEAWAY','DELIVERY','ROOM_SERVICE','BAR','APPOINTMENT','SUBSCRIPTION','SERVICE','HOTEL'));
UPDATE businesses SET hotel_enabled = TRUE, enabled_menus = enabled_menus || ',HOTEL'
WHERE UPPER(type) IN ('HOTEL', 'LODGE');
CREATE TABLE hotel_room_types (
 id VARCHAR(36) PRIMARY KEY, business_id VARCHAR(36) NOT NULL REFERENCES businesses(id),
 name VARCHAR(100) NOT NULL, description TEXT NOT NULL DEFAULT '', capacity INTEGER NOT NULL CHECK(capacity BETWEEN 1 AND 30),
 nightly_rate_cents BIGINT NOT NULL CHECK(nightly_rate_cents > 0), is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE hotel_rooms (
 id VARCHAR(36) PRIMARY KEY, business_id VARCHAR(36) NOT NULL REFERENCES businesses(id), type_id VARCHAR(36) NOT NULL REFERENCES hotel_room_types(id),
 room_number VARCHAR(50) NOT NULL, floor VARCHAR(50) NOT NULL DEFAULT '', housekeeping_status VARCHAR(30) NOT NULL DEFAULT 'CLEAN'
 CHECK(housekeeping_status IN ('CLEAN','DIRTY','IN_PROGRESS','INSPECTED','OUT_OF_SERVICE')),
 notes TEXT NOT NULL DEFAULT '', is_active BOOLEAN NOT NULL DEFAULT TRUE, updated_at TIMESTAMPTZ NOT NULL,
 UNIQUE(business_id, room_number)
);
CREATE TABLE hotel_reservations (
 id VARCHAR(36) PRIMARY KEY, business_id VARCHAR(36) NOT NULL REFERENCES businesses(id), room_id VARCHAR(36) NOT NULL REFERENCES hotel_rooms(id),
 guest_name VARCHAR(160) NOT NULL, guest_phone VARCHAR(30) NOT NULL DEFAULT '', guest_email VARCHAR(160) NOT NULL DEFAULT '', guests INTEGER NOT NULL CHECK(guests > 0),
 arrival DATE NOT NULL, departure DATE NOT NULL CHECK(departure > arrival), nightly_rate_cents BIGINT NOT NULL CHECK(nightly_rate_cents > 0),
 status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED' CHECK(status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED','NO_SHOW')),
 source VARCHAR(80) NOT NULL DEFAULT 'DIRECT', reference VARCHAR(120) NOT NULL DEFAULT '', notes TEXT NOT NULL DEFAULT '',
 checked_in_at TIMESTAMPTZ, checked_out_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_hotel_reservations_dates ON hotel_reservations(business_id,room_id,arrival,departure);
CREATE TABLE hotel_folio_entries (
 id VARCHAR(36) PRIMARY KEY, business_id VARCHAR(36) NOT NULL REFERENCES businesses(id), reservation_id VARCHAR(36) NOT NULL REFERENCES hotel_reservations(id),
 kind VARCHAR(30) NOT NULL CHECK(kind IN ('CHARGE','CREDIT','PAYMENT','CASH_REFUND')), description VARCHAR(255) NOT NULL, amount_cents BIGINT NOT NULL,
 payment_id VARCHAR(36) UNIQUE REFERENCES payments(id), order_id VARCHAR(36) UNIQUE REFERENCES orders(id),
 request_id VARCHAR(36) NOT NULL, actor_id VARCHAR(36) NOT NULL REFERENCES users(id), created_at TIMESTAMPTZ NOT NULL,
 UNIQUE(business_id,request_id)
);
CREATE INDEX idx_hotel_folio_reservation ON hotel_folio_entries(reservation_id);
CREATE TABLE hotel_room_blocks (
 id VARCHAR(36) PRIMARY KEY, business_id VARCHAR(36) NOT NULL REFERENCES businesses(id), room_id VARCHAR(36) NOT NULL REFERENCES hotel_rooms(id),
 arrival DATE NOT NULL, departure DATE NOT NULL CHECK(departure > arrival), reason VARCHAR(255) NOT NULL,
 source VARCHAR(80) NOT NULL DEFAULT 'MANUAL', external_uid VARCHAR(255) NOT NULL DEFAULT ''
);
CREATE INDEX idx_hotel_blocks_dates ON hotel_room_blocks(business_id,room_id,arrival,departure);
CREATE TABLE hotel_payment_requests (
 id VARCHAR(36) PRIMARY KEY, business_id VARCHAR(36) NOT NULL REFERENCES businesses(id), reservation_id VARCHAR(36) NOT NULL REFERENCES hotel_reservations(id),
 order_id VARCHAR(36) NOT NULL UNIQUE REFERENCES orders(id), request_id VARCHAR(36) NOT NULL, UNIQUE(business_id,request_id)
);
INSERT INTO permissions(id,code,module,action,name,description,created_at) VALUES
 ('perm_hotel_view','hotel.view','HOTEL','VIEW','View hotel','View rooms and reservations',NOW()),
 ('perm_hotel_manage','hotel.manage','HOTEL','MANAGE','Manage rooms','Configure room types, prices, rooms and calendar blocks',NOW()),
 ('perm_hotel_reserve','hotel.reservations','HOTEL','EDIT','Manage reservations','Create and change hotel reservations',NOW()),
 ('perm_hotel_frontdesk','hotel.frontdesk','HOTEL','EDIT','Hotel front desk','Check guests in and out',NOW()),
 ('perm_hotel_billing','hotel.billing','HOTEL','EDIT','Hotel billing','Post charges and collect payments',NOW()),
 ('perm_hotel_refunds','hotel.refunds','HOTEL','EDIT','Hotel credits and refunds','Post credits and record cash refunds',NOW()),
 ('perm_hotel_housekeeping','hotel.housekeeping','HOTEL','EDIT','Housekeeping','Update room cleaning status',NOW()),
 ('perm_hotel_reports','hotel.reports','HOTEL','VIEW','Hotel reports','Occupancy and accommodation reporting',NOW()),
 ('perm_hotel_channels','hotel.channels','HOTEL','MANAGE','Hotel calendars','Import and export channel availability calendars',NOW());
INSERT INTO role_permissions(role_id,permission_id)
 SELECT r.id,p.id FROM access_roles r CROSS JOIN permissions p WHERE LOWER(r.name) IN ('business owner','business admin','admin','manager') AND p.module='HOTEL'
 ON CONFLICT DO NOTHING;
UPDATE access_roles SET allowed_menus = allowed_menus || ',HOTEL' WHERE LOWER(name) IN ('business owner','business admin','admin','manager') AND POSITION('HOTEL' IN allowed_menus)=0;
