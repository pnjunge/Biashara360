-- Storefront orders are initially ordinary closed orders. Once staff claim them,
-- hospitality businesses manage them as active receipts in Open Tabs.
UPDATE orders o
SET tab_status = 'OPEN',
    delivery_status = 'PROCESSING',
    updated_at = CURRENT_TIMESTAMP
FROM businesses b
WHERE b.id = o.business_id
  AND (b.hospitality_enabled = TRUE OR UPPER(b.type) = 'HOSPITALITY')
  AND o.sales_channel = 'ECOMMERCE'
  AND o.server_user_id IS NOT NULL
  AND o.tab_status NOT IN ('OPEN', 'AWAITING_PAYMENT')
  AND o.payment_status NOT IN ('CANCELLED', 'VOIDED', 'REFUNDED')
  AND o.delivery_status NOT IN ('CANCELLED', 'DELIVERED', 'RETURNED');
