ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS sales_channel VARCHAR(30) NOT NULL DEFAULT 'WEB';

UPDATE orders
SET sales_channel = CASE
    WHEN order_number LIKE 'B360-DESK-%' THEN 'DESKTOP'
    WHEN order_number LIKE 'B360-ANDR-%' THEN 'ANDROID'
    WHEN order_number LIKE 'B360-IOS-%'  THEN 'IOS'
    WHEN order_number LIKE 'B360-SOC-%'  THEN 'SOCIAL'
    WHEN order_number LIKE 'B360-ECOM-%' THEN 'ECOMMERCE'
    ELSE 'WEB'
END;

CREATE INDEX IF NOT EXISTS idx_orders_business_channel_created
    ON orders (business_id, sales_channel, created_at DESC);
