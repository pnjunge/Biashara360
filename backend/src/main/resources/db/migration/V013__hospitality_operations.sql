ALTER TABLE businesses ADD COLUMN hospitality_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE orders ADD COLUMN service_type VARCHAR(20) NOT NULL DEFAULT 'RETAIL';
ALTER TABLE orders ADD COLUMN hospitality_table_id VARCHAR(36);
ALTER TABLE orders ADD COLUMN server_user_id VARCHAR(36);
ALTER TABLE orders ADD COLUMN guest_count INTEGER NOT NULL DEFAULT 1;
ALTER TABLE orders ADD COLUMN tab_status VARCHAR(20) NOT NULL DEFAULT 'CLOSED';

CREATE TABLE hospitality_tables (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(60) NOT NULL,
    area VARCHAR(80) NOT NULL DEFAULT 'Main Floor',
    capacity INTEGER NOT NULL DEFAULT 4 CHECK (capacity > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_hospitality_table_name UNIQUE (business_id, name)
);

ALTER TABLE orders ADD CONSTRAINT fk_orders_hospitality_table
    FOREIGN KEY (hospitality_table_id) REFERENCES hospitality_tables(id) ON DELETE SET NULL;
ALTER TABLE orders ADD CONSTRAINT fk_orders_server_user
    FOREIGN KEY (server_user_id) REFERENCES users(id) ON DELETE SET NULL;

CREATE TABLE kitchen_tickets (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    order_id VARCHAR(36) NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    station VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    notes VARCHAR(500) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_kitchen_ticket_order_station UNIQUE (order_id, station)
);

CREATE INDEX idx_hospitality_tables_business_status ON hospitality_tables (business_id, status);
CREATE INDEX idx_orders_hospitality_open_tabs ON orders (business_id, tab_status, created_at DESC);
CREATE INDEX idx_kitchen_tickets_business_status ON kitchen_tickets (business_id, status, created_at);
