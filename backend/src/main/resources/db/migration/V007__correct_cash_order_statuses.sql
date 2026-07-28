-- Cash is collected immediately, so historical cash orders must not remain unpaid.
UPDATE orders
SET payment_status = 'PAID',
    updated_at = CURRENT_TIMESTAMP
WHERE UPPER(payment_method) = 'CASH'
  AND UPPER(payment_status) = 'PENDING';

-- Only repair delivery for clearly in-store/POS cash sales. Cash orders with an
-- actual delivery location remain pending until the merchant fulfils them.
UPDATE orders
SET delivery_status = 'DELIVERED',
    updated_at = CURRENT_TIMESTAMP
WHERE UPPER(payment_method) = 'CASH'
  AND UPPER(delivery_status) = 'PENDING'
  AND (
      LOWER(TRIM(delivery_location)) IN ('', 'pos', 'in-store pos', 'in store pos')
      OR LOWER(notes) LIKE '%pos checkout%'
  );
