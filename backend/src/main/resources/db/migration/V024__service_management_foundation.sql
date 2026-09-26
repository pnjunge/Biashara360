CREATE TABLE IF NOT EXISTS business_services (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT '',
    category VARCHAR(80) NOT NULL DEFAULT '',
    duration_minutes INTEGER NOT NULL DEFAULT 60 CHECK (duration_minutes > 0),
    price DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (price >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_business_service_name UNIQUE (business_id, name)
);

CREATE INDEX IF NOT EXISTS idx_business_services_business_name ON business_services(business_id, name);

CREATE TABLE IF NOT EXISTS service_resources (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'RESOURCE',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_service_resource_name UNIQUE (business_id, name)
);

CREATE INDEX IF NOT EXISTS idx_service_resources_business_name ON service_resources(business_id, name);

CREATE TABLE IF NOT EXISTS service_appointments (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    service_id VARCHAR(36) NOT NULL REFERENCES business_services(id) ON DELETE CASCADE,
    resource_id VARCHAR(36) REFERENCES service_resources(id) ON DELETE SET NULL,
    customer_id VARCHAR(36) REFERENCES customers(id) ON DELETE SET NULL,
    staff_user_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL DEFAULT '',
    starts_at TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 60 CHECK (duration_minutes > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED',
    notes VARCHAR(500) NOT NULL DEFAULT '',
    order_id VARCHAR(36) REFERENCES orders(id) ON DELETE SET NULL,
    created_by VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_service_appointments_business_starts ON service_appointments(business_id, starts_at);
CREATE INDEX IF NOT EXISTS idx_service_appointments_staff_schedule ON service_appointments(business_id, staff_user_id, starts_at);
CREATE INDEX IF NOT EXISTS idx_service_appointments_resource_schedule ON service_appointments(business_id, resource_id, starts_at);

UPDATE businesses
SET enabled_menus = CASE
    WHEN POSITION('SERVICES' IN enabled_menus) = 0 THEN enabled_menus || ',SERVICES'
    ELSE enabled_menus
END;
