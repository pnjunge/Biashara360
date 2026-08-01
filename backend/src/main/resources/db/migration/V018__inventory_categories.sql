CREATE TABLE inventory_categories (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_inventory_categories_business_name
    ON inventory_categories (business_id, LOWER(name));

CREATE INDEX idx_inventory_categories_business_active
    ON inventory_categories (business_id, is_active);

INSERT INTO inventory_categories (id, business_id, name)
SELECT
    SUBSTRING(hash, 1, 8) || '-' || SUBSTRING(hash, 9, 4) || '-' || SUBSTRING(hash, 13, 4) || '-' ||
    SUBSTRING(hash, 17, 4) || '-' || SUBSTRING(hash, 21, 12),
    business_id,
    category
FROM (
    SELECT business_id, MIN(TRIM(category)) AS category,
           MD5(business_id || ':inventory-category:' || LOWER(TRIM(category))) AS hash
    FROM products
    WHERE TRIM(category) <> ''
    GROUP BY business_id, LOWER(TRIM(category))
) existing_categories
ON CONFLICT DO NOTHING;
