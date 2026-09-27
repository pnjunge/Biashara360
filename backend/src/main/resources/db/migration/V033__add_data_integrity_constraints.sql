-- V033: Add database constraints for data integrity and security
-- 🔒 SECURITY FIX: Enforce data validation at database level

-- ============================================================================
-- AMOUNT CONSTRAINTS: Prevent negative amounts and invalid financial data
-- ============================================================================

-- Products: Prices must be non-negative
ALTER TABLE products
    ADD CONSTRAINT chk_products_buying_price_positive
    CHECK (buying_price >= 0);

ALTER TABLE products
    ADD CONSTRAINT chk_products_selling_price_positive
    CHECK (selling_price >= 0);

ALTER TABLE products
    ADD CONSTRAINT chk_products_stock_non_negative
    CHECK (current_stock >= 0);

ALTER TABLE products
    ADD CONSTRAINT chk_products_threshold_non_negative
    CHECK (low_stock_threshold >= 0);

-- Orders: Amounts must be non-negative
ALTER TABLE orders
    ADD CONSTRAINT chk_orders_base_amount_non_negative
    CHECK (base_amount >= 0);

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_tax_rate_valid
    CHECK (tax_rate >= 0 AND tax_rate <= 1.0);

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_tax_amount_non_negative
    CHECK (tax_amount >= 0);

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_subtotal_positive
    CHECK (subtotal > 0);

-- Order Items: Quantities and prices must be valid
ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_quantity_positive
    CHECK (quantity > 0);

ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_unit_price_non_negative
    CHECK (unit_price >= 0);

ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_buying_price_non_negative
    CHECK (buying_price >= 0);

ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_discount_non_negative
    CHECK (discount_amount >= 0);

-- Payments: Amounts must be positive
ALTER TABLE payments
    ADD CONSTRAINT chk_payments_amount_positive
    CHECK (amount > 0);

-- Expenses: Amounts must be positive
ALTER TABLE expenses
    ADD CONSTRAINT chk_expenses_amount_positive
    CHECK (amount > 0);

-- CyberSource Transactions: Amounts must be positive
ALTER TABLE cybersource_transactions
    ADD CONSTRAINT chk_cs_transactions_amount_positive
    CHECK (amount > 0);

-- Business Services: Prices must be non-negative
ALTER TABLE business_services
    ADD CONSTRAINT chk_business_services_price_non_negative
    CHECK (price >= 0);

ALTER TABLE business_services
    ADD CONSTRAINT chk_business_services_duration_positive
    CHECK (duration_minutes > 0);

-- Service Appointments: Duration must be positive
ALTER TABLE service_appointments
    ADD CONSTRAINT chk_service_appointments_duration_positive
    CHECK (duration_minutes > 0);

ALTER TABLE service_appointments
    ADD CONSTRAINT chk_service_appointments_guest_count_positive
    CHECK (guest_count >= 1);

-- Customers: Loyalty points must be non-negative
ALTER TABLE customers
    ADD CONSTRAINT chk_customers_loyalty_points_non_negative
    CHECK (loyalty_points >= 0);

-- ============================================================================
-- PHONE NUMBER FORMAT CONSTRAINTS: Enforce Kenya phone number format
-- ============================================================================

-- Users: Phone must match Kenya format (+254... or 254... or 07...)
ALTER TABLE users
    ADD CONSTRAINT chk_users_phone_format
    CHECK (phone ~ '^\+?254[17]\d{8}$|^0[17]\d{8}$');

-- Businesses: Owner phone must match Kenya format
ALTER TABLE businesses
    ADD CONSTRAINT chk_businesses_owner_phone_format
    CHECK (owner_phone ~ '^\+?254[17]\d{8}$|^0[17]\d{8}$');

-- Customers: Phone must match Kenya format (if provided)
ALTER TABLE customers
    DROP CONSTRAINT IF EXISTS chk_customers_phone_format;

ALTER TABLE customers
    ADD CONSTRAINT chk_customers_phone_format
    CHECK (phone = '' OR phone ~ '^\+?254[17]\d{8}$|^0[17]\d{8}$');

-- ============================================================================
-- EMAIL FORMAT CONSTRAINTS: Basic email validation
-- ============================================================================

-- Users: Email must be valid format
ALTER TABLE users
    ADD CONSTRAINT chk_users_email_format
    CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- Businesses: Owner email must be valid format
ALTER TABLE businesses
    ADD CONSTRAINT chk_businesses_owner_email_format
    CHECK (owner_email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- Customers: Email must be valid format (if provided)
ALTER TABLE customers
    DROP CONSTRAINT IF EXISTS chk_customers_email_format;

ALTER TABLE customers
    ADD CONSTRAINT chk_customers_email_format
    CHECK (email IS NULL OR email = '' OR email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- ============================================================================
-- ENUM/STATUS CONSTRAINTS: Ensure only valid values
-- ============================================================================

-- Orders: Payment status must be valid
ALTER TABLE orders
    ADD CONSTRAINT chk_orders_payment_status
    CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED', 'COD', 'PARTIAL'));

-- Orders: Delivery status must be valid
ALTER TABLE orders
    ADD CONSTRAINT chk_orders_delivery_status
    CHECK (delivery_status IN ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'));

-- Orders: Payment method must be valid
ALTER TABLE orders
    ADD CONSTRAINT chk_orders_payment_method
    CHECK (payment_method IN ('CASH', 'MPESA', 'CARD', 'BANK_TRANSFER', 'COD', 'CREDIT'));

-- Orders: Tab status must be valid
ALTER TABLE orders
    ADD CONSTRAINT chk_orders_tab_status
    CHECK (tab_status IN ('OPEN', 'CLOSED', 'MERGED', 'SPLIT'));

-- Orders: Service type must be valid
ALTER TABLE orders
    ADD CONSTRAINT chk_orders_service_type
    CHECK (service_type IN ('RETAIL', 'DINE_IN', 'TAKEAWAY', 'DELIVERY', 'ROOM_SERVICE', 'BAR', 'APPOINTMENT'));

-- Payments: Status must be valid
ALTER TABLE payments
    ADD CONSTRAINT chk_payments_status
    CHECK (status IN ('SUCCESS', 'PENDING', 'FAILED', 'CANCELLED', 'REFUNDED'));

-- Payments: Method must be valid
ALTER TABLE payments
    ADD CONSTRAINT chk_payments_method
    CHECK (method IN ('CASH', 'MPESA', 'CARD', 'BANK_TRANSFER', 'MOBILE_MONEY', 'CREDIT'));

-- Users: Role must be valid
ALTER TABLE users
    ADD CONSTRAINT chk_users_role
    CHECK (role IN ('SUPERADMIN', 'ADMIN', 'MANAGER', 'STAFF', 'CASHIER', 'WAITER', 'CHEF', 'BARTENDER'));

-- CyberSource Transactions: Status must be valid
ALTER TABLE cybersource_transactions
    ADD CONSTRAINT chk_cs_transactions_status
    CHECK (status IN ('AUTHORIZED', 'DECLINED', 'CAPTURED', 'REFUNDED', 'VOIDED', 'ERROR', 'PENDING'));

-- CyberSource Transactions: Type must be valid
ALTER TABLE cybersource_transactions
    ADD CONSTRAINT chk_cs_transactions_type
    CHECK (transaction_type IN ('AUTHORIZATION', 'CAPTURE', 'REFUND', 'VOID', 'SALE'));

-- Service Appointments: Status must be valid
ALTER TABLE service_appointments
    ADD CONSTRAINT chk_service_appointments_status
    CHECK (status IN ('BOOKED', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW'));

-- ============================================================================
-- BUSINESS LOGIC CONSTRAINTS
-- ============================================================================

-- Businesses: Max users must be positive
ALTER TABLE businesses
    ADD CONSTRAINT chk_businesses_max_users_positive
    CHECK (max_users > 0);

-- Hospitality Tables: Capacity must be positive
ALTER TABLE hospitality_tables
    ADD CONSTRAINT chk_hospitality_tables_capacity_positive
    CHECK (capacity > 0);

-- Stock Movements: Type must be valid
ALTER TABLE stock_movements
    ADD CONSTRAINT chk_stock_movements_type
    CHECK (type IN ('STOCK_IN', 'STOCK_OUT', 'ADJUSTMENT', 'TRANSFER', 'RETURN', 'DAMAGE'));

-- OTP: Code must be 6 digits
ALTER TABLE otp_codes
    ADD CONSTRAINT chk_otp_code_format
    CHECK (code ~ '^\d{6}$');

-- OTP: Channel must be valid
ALTER TABLE otp_codes
    ADD CONSTRAINT chk_otp_channel
    CHECK (channel IN ('SMS', 'EMAIL', 'WHATSAPP', 'APP', 'PASSWORD_RESET'));

-- ============================================================================
-- STRING LENGTH CONSTRAINTS (prevent buffer overflow attacks)
-- ============================================================================

-- Prevent excessively long strings that could indicate attack
ALTER TABLE products
    ADD CONSTRAINT chk_products_sku_length
    CHECK (length(sku) <= 100);

ALTER TABLE products
    ADD CONSTRAINT chk_products_name_length
    CHECK (length(name) BETWEEN 1 AND 255);

ALTER TABLE businesses
    ADD CONSTRAINT chk_businesses_name_length
    CHECK (length(name) BETWEEN 2 AND 255);

ALTER TABLE users
    ADD CONSTRAINT chk_users_name_length
    CHECK (length(name) BETWEEN 2 AND 255);

-- ============================================================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================================================

COMMENT ON CONSTRAINT chk_products_buying_price_positive ON products IS
'🔒 SECURITY: Prevents negative prices that could break financial calculations';

COMMENT ON CONSTRAINT chk_orders_subtotal_positive ON orders IS
'🔒 SECURITY: Orders must have positive total amount';

COMMENT ON CONSTRAINT chk_payments_amount_positive ON payments IS
'🔒 SECURITY: Payment amounts must be positive';

COMMENT ON CONSTRAINT chk_users_phone_format ON users IS
'🔒 SECURITY: Enforces Kenya phone number format (+254... or 07...)';

COMMENT ON CONSTRAINT chk_users_email_format ON users IS
'🔒 SECURITY: Prevents invalid email addresses';

COMMENT ON CONSTRAINT chk_orders_payment_status ON orders IS
'🔒 SECURITY: Only valid payment statuses allowed';

-- ============================================================================
-- INDEXES FOR CONSTRAINT PERFORMANCE
-- ============================================================================

-- These indexes help constraint checks perform faster
CREATE INDEX IF NOT EXISTS idx_users_phone_partial ON users(phone) WHERE phone IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_email_partial ON users(email) WHERE email IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_customers_phone_partial ON customers(phone) WHERE phone != '';
CREATE INDEX IF NOT EXISTS idx_customers_email_partial ON customers(email) WHERE email IS NOT NULL AND email != '';

-- ============================================================================
-- VALIDATION NOTES
-- ============================================================================

-- Note: Existing data that violates these constraints will prevent migration.
-- Run data cleanup before applying this migration:
-- 1. Fix negative amounts: UPDATE products SET buying_price = 0 WHERE buying_price < 0;
-- 2. Fix invalid phones: UPDATE users SET phone = '0700000000' WHERE phone !~ phone_regex;
-- 3. Fix invalid emails: UPDATE users SET email = 'noreply@biashara360.co.ke' WHERE email !~* email_regex;
