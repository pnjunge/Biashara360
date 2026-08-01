-- Accelerate the tenant-scoped contains searches used by product and customer lists.
-- PostgreSQL can combine these GIN indexes with the business_id indexes using a bitmap scan.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_products_active_business
    ON products (business_id)
    WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_products_name_trgm
    ON products USING GIN (LOWER(name) gin_trgm_ops)
    WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_products_sku_trgm
    ON products USING GIN (LOWER(sku) gin_trgm_ops)
    WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_customers_active_business
    ON customers (business_id)
    WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_customers_name_trgm
    ON customers USING GIN (LOWER(name) gin_trgm_ops)
    WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_customers_phone_trgm
    ON customers USING GIN (phone gin_trgm_ops)
    WHERE is_active = TRUE;
