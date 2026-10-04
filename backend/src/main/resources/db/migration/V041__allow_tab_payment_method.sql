-- Fix chk_orders_payment_method constraint to support TAB and COD payment methods
ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_payment_method;
ALTER TABLE orders ADD CONSTRAINT chk_orders_payment_method
    CHECK (payment_method IN ('CASH', 'MPESA', 'CARD', 'BANK_TRANSFER', 'COD', 'CREDIT', 'TAB'));
