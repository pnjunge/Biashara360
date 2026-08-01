ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS base_amount DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS tax_included BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS tax_rate DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS tax_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0;

UPDATE orders
SET base_amount = subtotal
WHERE base_amount IS NULL;

ALTER TABLE orders
    ALTER COLUMN base_amount SET NOT NULL;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_tax_rate
        CHECK (tax_rate >= 0.0 AND tax_rate <= 1.0),
    ADD CONSTRAINT chk_orders_tax_amount
        CHECK (tax_amount >= 0.0);
