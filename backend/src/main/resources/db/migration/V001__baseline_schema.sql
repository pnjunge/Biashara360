--
-- PostgreSQL database dump
--


-- Dumped from database version 18.3
-- Dumped by pg_dump version 18.4 (Ubuntu 18.4-1.pgdg25.10+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: business_session_settings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.business_session_settings (
    business_id character varying(36) NOT NULL,
    web_timeout_seconds bigint DEFAULT 1800 NOT NULL,
    android_timeout_seconds bigint DEFAULT 1800 NOT NULL,
    desktop_timeout_seconds bigint DEFAULT 1800 NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: businesses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.businesses (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    type character varying(50) NOT NULL,
    owner_name character varying(255),
    owner_phone character varying(20) NOT NULL,
    owner_email character varying(255) NOT NULL,
    county character varying(100),
    address character varying(500),
    kra_pin character varying(20),
    paybill_number character varying(20),
    account_number character varying(50),
    mpesa_short_code character varying(20),
    currency character varying(10) DEFAULT 'KES'::character varying NOT NULL,
    subscription_tier character varying(20) DEFAULT 'FREEMIUM'::character varying NOT NULL,
    enabled_modules text DEFAULT 'INVENTORY,SALES,CRM,EXPENSES,PAYMENTS,REPORTS'::text NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    receipt_header character varying(255) DEFAULT 'Welcome to our store!'::character varying NOT NULL,
    receipt_footer character varying(255) DEFAULT 'Thank you for shopping with us!'::character varying NOT NULL,
    receipt_show_tax boolean DEFAULT true NOT NULL,
    receipt_show_customer boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: cs_customer_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cs_customer_tokens (
    id character varying(36) NOT NULL,
    business_id character varying(36) NOT NULL,
    customer_id character varying(36),
    cs_customer_id character varying(255) NOT NULL,
    card_last4 character varying(4) NOT NULL,
    card_type character varying(20) NOT NULL,
    expiry_month character varying(2) NOT NULL,
    expiry_year character varying(4) NOT NULL,
    holder_name character varying(255) NOT NULL,
    is_default boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone NOT NULL
);


--
-- Name: customers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customers (
    id character varying(36) NOT NULL,
    business_id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    phone character varying(20) NOT NULL,
    email character varying(255),
    location character varying(255) DEFAULT ''::character varying NOT NULL,
    notes text DEFAULT ''::text NOT NULL,
    loyalty_points integer DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: cybersource_configs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cybersource_configs (
    id character varying(36) NOT NULL,
    business_id character varying(36) NOT NULL,
    merchant_id character varying(255) NOT NULL,
    merchant_key_id character varying(255) NOT NULL,
    merchant_secret_key text NOT NULL,
    environment character varying(20) DEFAULT 'sandbox'::character varying NOT NULL,
    encryption_version integer DEFAULT 1 NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: cybersource_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cybersource_transactions (
    id character varying(36) NOT NULL,
    business_id character varying(36) NOT NULL,
    order_id character varying(36),
    payment_id character varying(36),
    cs_transaction_id character varying(100),
    cs_reconciliation_id character varying(100),
    cs_approval_code character varying(50),
    amount double precision NOT NULL,
    currency character varying(10) DEFAULT 'KES'::character varying NOT NULL,
    card_last4 character varying(4),
    card_type character varying(20),
    cardholder_name character varying(255),
    transaction_type character varying(30) NOT NULL,
    status character varying(30) NOT NULL,
    processor_response character varying(10),
    error_reason character varying(100),
    error_message text,
    customer_token_id character varying(255),
    client_reference character varying(100),
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: etims_invoices; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.etims_invoices (
    id character varying(36) NOT NULL,
    business_id character varying(36) NOT NULL,
    order_id character varying(36),
    invoice_number character varying(50) NOT NULL,
    etims_invoice_number character varying(50),
    receipt_type character varying(5) DEFAULT 'NS'::character varying NOT NULL,
    payment_type character varying(5) DEFAULT '01'::character varying NOT NULL,
    taxable_amount double precision NOT NULL,
    tax_amount double precision NOT NULL,
    total_amount double precision NOT NULL,
    qr_code_content text,
    qr_code_base64 text,
    sdc_id character varying(100),
    sdc_date_time character varying(30),
    intrl_data text,
    rcpt_sign text,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    retry_count integer DEFAULT 0 NOT NULL,
    error_message text,
    raw_response text,
    submitted_at timestamp without time zone,
    created_at timestamp without time zone NOT NULL
);


--
-- Name: expenses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.expenses (
    id character varying(36) NOT NULL,
    business_id character varying(36) NOT NULL,
    category character varying(50) NOT NULL,
    amount double precision NOT NULL,
    description text NOT NULL,
    receipt_url character varying(500),
    expense_date date NOT NULL,
    recorded_at timestamp without time zone NOT NULL
);


--
-- Name: kra_profiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.kra_profiles (
    id character varying(36) NOT NULL,
    business_id character varying(36) NOT NULL,
    kra_pin character varying(20) NOT NULL,
    company_name character varying(255) NOT NULL,
    vat_number character varying(20),
    etims_sdc_id character varying(100),
    etims_device_serial_no character varying(100),
    etims_environment character varying(20) DEFAULT 'sandbox'::character varying NOT NULL,
    is_verified boolean DEFAULT false NOT NULL,
    last_sync_at timestamp without time zone,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: mpesa_configs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mpesa_configs (
    id character varying(36) NOT NULL,
    business_id character varying(36) NOT NULL,
    consumer_key text NOT NULL,
    consumer_secret text NOT NULL,
    short_code character varying(20) NOT NULL,
    pass_key text NOT NULL,
    callback_url character varying(500) NOT NULL,
    environment character varying(20) DEFAULT 'sandbox'::character varying NOT NULL,
    account_type character varying(10) DEFAULT 'paybill'::character varying NOT NULL,
    encryption_version integer DEFAULT 1 NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: order_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_items (
    id character varying(36) NOT NULL,
    order_id character varying(36) NOT NULL,
    product_id character varying(36) NOT NULL,
    product_name character varying(255) NOT NULL,
    quantity integer NOT NULL,
    unit_price double precision NOT NULL,
    buying_price double precision NOT NULL
);


--
-- Name: order_tax_lines; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_tax_lines (
    id character varying(36) NOT NULL,
    order_id character varying(36) NOT NULL,
    tax_rate_id character varying(36) NOT NULL,
    tax_type character varying(30) NOT NULL,
    tax_name character varying(100) NOT NULL,
    rate double precision NOT NULL,
    taxable_amount double precision NOT NULL,
    tax_amount double precision NOT NULL
);


--
-- Name: orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orders (
    id character varying(36) NOT NULL,
    order_number character varying(20) NOT NULL,
    business_id character varying(36) NOT NULL,
    customer_id character varying(36),
    customer_name character varying(255) NOT NULL,
    customer_phone character varying(20) NOT NULL,
    delivery_location text DEFAULT ''::text NOT NULL,
    payment_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    delivery_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    payment_method character varying(30) DEFAULT 'MPESA'::character varying NOT NULL,
    mpesa_transaction_code character varying(50),
    stk_checkout_request_id character varying(100),
    notes text DEFAULT ''::text NOT NULL,
    subtotal double precision NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    client_reference character varying(64)
);


--
-- Name: otp_codes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.otp_codes (
    id character varying(36) NOT NULL,
    user_id character varying(36) NOT NULL,
    code character varying(10) NOT NULL,
    channel character varying(20) NOT NULL,
    used boolean DEFAULT false NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    created_at timestamp without time zone NOT NULL
);


--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    id character varying(36) NOT NULL,
    business_id character varying(36) NOT NULL,
    order_id character varying(36),
    transaction_code character varying(50) NOT NULL,
    amount double precision NOT NULL,
    payer_phone character varying(20) NOT NULL,
    payer_name character varying(255) NOT NULL,
    method character varying(30) NOT NULL,
    status character varying(20) NOT NULL,
    channel character varying(50) NOT NULL,
    reconciled boolean DEFAULT false NOT NULL,
    notes text DEFAULT ''::text NOT NULL,
    transaction_date timestamp without time zone NOT NULL
);


--
-- Name: products; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.products (
    id character varying(36) NOT NULL,
    business_id character varying(36) NOT NULL,
    sku character varying(100) NOT NULL,
    name character varying(255) NOT NULL,
    description text DEFAULT ''::text NOT NULL,
    buying_price double precision NOT NULL,
    selling_price double precision NOT NULL,
    current_stock integer DEFAULT 0 NOT NULL,
    low_stock_threshold integer DEFAULT 5 NOT NULL,
    category character varying(100) DEFAULT ''::character varying NOT NULL,
    image_url character varying(500),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    barcode character varying(100)
);


--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_tokens (
    id character varying(36) NOT NULL,
    user_id character varying(36) NOT NULL,
    token character varying(512) NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    created_at timestamp without time zone NOT NULL
);


--
-- Name: social_channels; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.social_channels (
    id character varying(36) NOT NULL,
    business_id character varying(36) NOT NULL,
    platform character varying(20) NOT NULL,
    channel_name character varying(255) NOT NULL,
    external_id character varying(255) NOT NULL,
    phone_number character varying(30),
    access_token text NOT NULL,
    refresh_token text,
    token_expires_at timestamp without time zone,
    webhook_verify_token character varying(100) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    auto_reply_enabled boolean DEFAULT true NOT NULL,
    ai_persona_prompt text DEFAULT ''::text NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    tenant_id character varying(64),
    waba_id character varying(255),
    phone_number_id character varying(255),
    meta_business_id character varying(255)
);


--
-- Name: social_conversations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.social_conversations (
    id character varying(36) NOT NULL,
    business_id character varying(36) NOT NULL,
    channel_id character varying(36) NOT NULL,
    platform character varying(20) NOT NULL,
    external_conv_id character varying(255) NOT NULL,
    customer_external_id character varying(255) NOT NULL,
    customer_name character varying(255) DEFAULT 'Unknown'::character varying NOT NULL,
    customer_phone character varying(30),
    customer_id character varying(36),
    status character varying(20) DEFAULT 'OPEN'::character varying NOT NULL,
    assigned_order_id character varying(36),
    last_message_at timestamp without time zone NOT NULL,
    unread_count integer DEFAULT 0 NOT NULL,
    is_ai_handled boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone NOT NULL
);


--
-- Name: social_messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.social_messages (
    id character varying(36) NOT NULL,
    conversation_id character varying(36) NOT NULL,
    business_id character varying(36) NOT NULL,
    external_msg_id character varying(255),
    direction character varying(10) NOT NULL,
    sender_type character varying(15) NOT NULL,
    content text NOT NULL,
    message_type character varying(20) DEFAULT 'TEXT'::character varying NOT NULL,
    media_url text,
    status character varying(20) DEFAULT 'SENT'::character varying NOT NULL,
    is_ai_generated boolean DEFAULT false NOT NULL,
    metadata text,
    created_at timestamp without time zone NOT NULL
);


--
-- Name: social_orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.social_orders (
    id character varying(36) NOT NULL,
    business_id character varying(36) NOT NULL,
    conversation_id character varying(36) NOT NULL,
    order_id character varying(36) NOT NULL,
    platform character varying(20) NOT NULL,
    payment_link_sent boolean DEFAULT false NOT NULL,
    payment_link_sent_at timestamp without time zone,
    created_at timestamp without time zone NOT NULL
);


--
-- Name: stock_movements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stock_movements (
    id character varying(36) NOT NULL,
    product_id character varying(36) NOT NULL,
    business_id character varying(36) NOT NULL,
    type character varying(20) NOT NULL,
    quantity integer NOT NULL,
    note text DEFAULT ''::text NOT NULL,
    order_id character varying(36),
    recorded_at timestamp without time zone NOT NULL
);


--
-- Name: system_settings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.system_settings (
    key character varying(100) NOT NULL,
    value text NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: tax_rates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tax_rates (
    id character varying(36) NOT NULL,
    business_id character varying(36) NOT NULL,
    tax_type character varying(30) NOT NULL,
    name character varying(100) NOT NULL,
    rate double precision NOT NULL,
    is_inclusive boolean DEFAULT false NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    applies_to character varying(50) DEFAULT 'ALL'::character varying NOT NULL,
    description text DEFAULT ''::text NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: tax_remittances; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tax_remittances (
    id character varying(36) NOT NULL,
    business_id character varying(36) NOT NULL,
    tax_type character varying(30) NOT NULL,
    period_start date NOT NULL,
    period_end date NOT NULL,
    taxable_amount double precision NOT NULL,
    tax_amount double precision NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    receipt_number character varying(100),
    filed_at timestamp without time zone,
    paid_at timestamp without time zone,
    notes text DEFAULT ''::text NOT NULL,
    created_at timestamp without time zone NOT NULL
);


--
-- Name: tax_returns; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tax_returns (
    id character varying(36) NOT NULL,
    business_id character varying(36) NOT NULL,
    return_type character varying(10) NOT NULL,
    period_year integer NOT NULL,
    period_month integer NOT NULL,
    standard_rated_sales double precision DEFAULT 0.0 NOT NULL,
    zero_rated_sales double precision DEFAULT 0.0 NOT NULL,
    exempt_sales double precision DEFAULT 0.0 NOT NULL,
    output_vat double precision DEFAULT 0.0 NOT NULL,
    input_vat double precision DEFAULT 0.0 NOT NULL,
    net_vat_payable double precision DEFAULT 0.0 NOT NULL,
    gross_receipts double precision DEFAULT 0.0 NOT NULL,
    tax_amount double precision DEFAULT 0.0 NOT NULL,
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    itax_acknowledgement_no character varying(100),
    csv_file_name character varying(255),
    submitted_at timestamp without time zone,
    generated_at timestamp without time zone,
    created_at timestamp without time zone NOT NULL
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id character varying(36) NOT NULL,
    business_id character varying(36),
    name character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    phone character varying(20) NOT NULL,
    password_hash character varying(255) NOT NULL,
    role character varying(20) DEFAULT 'STAFF'::character varying NOT NULL,
    two_factor_enabled boolean DEFAULT false NOT NULL,
    preferred_language character varying(10) DEFAULT 'ENGLISH'::character varying NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: business_session_settings business_session_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business_session_settings
    ADD CONSTRAINT business_session_settings_pkey PRIMARY KEY (business_id);


--
-- Name: businesses businesses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.businesses
    ADD CONSTRAINT businesses_pkey PRIMARY KEY (id);


--
-- Name: cs_customer_tokens cs_customer_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cs_customer_tokens
    ADD CONSTRAINT cs_customer_tokens_pkey PRIMARY KEY (id);


--
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (id);


--
-- Name: cybersource_configs cybersource_configs_business_id_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cybersource_configs
    ADD CONSTRAINT cybersource_configs_business_id_unique UNIQUE (business_id);


--
-- Name: cybersource_configs cybersource_configs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cybersource_configs
    ADD CONSTRAINT cybersource_configs_pkey PRIMARY KEY (id);


--
-- Name: cybersource_transactions cybersource_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cybersource_transactions
    ADD CONSTRAINT cybersource_transactions_pkey PRIMARY KEY (id);


--
-- Name: etims_invoices etims_invoices_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etims_invoices
    ADD CONSTRAINT etims_invoices_pkey PRIMARY KEY (id);


--
-- Name: expenses expenses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT expenses_pkey PRIMARY KEY (id);


--
-- Name: orders idx_orders_business_client_reference; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT idx_orders_business_client_reference UNIQUE (business_id, client_reference);


--
-- Name: kra_profiles kra_profiles_business_id_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kra_profiles
    ADD CONSTRAINT kra_profiles_business_id_unique UNIQUE (business_id);


--
-- Name: kra_profiles kra_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kra_profiles
    ADD CONSTRAINT kra_profiles_pkey PRIMARY KEY (id);


--
-- Name: mpesa_configs mpesa_configs_business_id_account_type_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mpesa_configs
    ADD CONSTRAINT mpesa_configs_business_id_account_type_unique UNIQUE (business_id, account_type);


--
-- Name: mpesa_configs mpesa_configs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mpesa_configs
    ADD CONSTRAINT mpesa_configs_pkey PRIMARY KEY (id);


--
-- Name: order_items order_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id);


--
-- Name: order_tax_lines order_tax_lines_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_tax_lines
    ADD CONSTRAINT order_tax_lines_pkey PRIMARY KEY (id);


--
-- Name: orders orders_order_number_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_order_number_unique UNIQUE (order_number);


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);


--
-- Name: otp_codes otp_codes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.otp_codes
    ADD CONSTRAINT otp_codes_pkey PRIMARY KEY (id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_token_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_token_unique UNIQUE (token);


--
-- Name: social_channels social_channels_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.social_channels
    ADD CONSTRAINT social_channels_pkey PRIMARY KEY (id);


--
-- Name: social_conversations social_conversations_channel_id_external_conv_id_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.social_conversations
    ADD CONSTRAINT social_conversations_channel_id_external_conv_id_unique UNIQUE (channel_id, external_conv_id);


--
-- Name: social_conversations social_conversations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.social_conversations
    ADD CONSTRAINT social_conversations_pkey PRIMARY KEY (id);


--
-- Name: social_messages social_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.social_messages
    ADD CONSTRAINT social_messages_pkey PRIMARY KEY (id);


--
-- Name: social_orders social_orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.social_orders
    ADD CONSTRAINT social_orders_pkey PRIMARY KEY (id);


--
-- Name: stock_movements stock_movements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_movements
    ADD CONSTRAINT stock_movements_pkey PRIMARY KEY (id);


--
-- Name: system_settings system_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.system_settings
    ADD CONSTRAINT system_settings_pkey PRIMARY KEY (key);


--
-- Name: tax_rates tax_rates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_rates
    ADD CONSTRAINT tax_rates_pkey PRIMARY KEY (id);


--
-- Name: tax_remittances tax_remittances_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_remittances
    ADD CONSTRAINT tax_remittances_pkey PRIMARY KEY (id);


--
-- Name: tax_returns tax_returns_business_id_return_type_period_year_period_month_un; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_returns
    ADD CONSTRAINT tax_returns_business_id_return_type_period_year_period_month_un UNIQUE (business_id, return_type, period_year, period_month);


--
-- Name: tax_returns tax_returns_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_returns
    ADD CONSTRAINT tax_returns_pkey PRIMARY KEY (id);


--
-- Name: users users_email_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_unique UNIQUE (email);


--
-- Name: users users_phone_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_phone_unique UNIQUE (phone);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: idx_orders_business_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_business_created ON public.orders USING btree (business_id, created_at);


--
-- Name: social_channels_platform_phone_number_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX social_channels_platform_phone_number_id ON public.social_channels USING btree (platform, phone_number_id);


--
-- Name: social_channels_platform_waba_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX social_channels_platform_waba_id ON public.social_channels USING btree (platform, waba_id);


--
-- Name: business_session_settings fk_business_session_settings_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business_session_settings
    ADD CONSTRAINT fk_business_session_settings_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: cs_customer_tokens fk_cs_customer_tokens_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cs_customer_tokens
    ADD CONSTRAINT fk_cs_customer_tokens_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: customers fk_customers_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT fk_customers_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: cybersource_configs fk_cybersource_configs_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cybersource_configs
    ADD CONSTRAINT fk_cybersource_configs_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: cybersource_transactions fk_cybersource_transactions_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cybersource_transactions
    ADD CONSTRAINT fk_cybersource_transactions_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: etims_invoices fk_etims_invoices_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etims_invoices
    ADD CONSTRAINT fk_etims_invoices_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: expenses fk_expenses_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT fk_expenses_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: kra_profiles fk_kra_profiles_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kra_profiles
    ADD CONSTRAINT fk_kra_profiles_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: mpesa_configs fk_mpesa_configs_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mpesa_configs
    ADD CONSTRAINT fk_mpesa_configs_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: order_items fk_order_items_order_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT fk_order_items_order_id__id FOREIGN KEY (order_id) REFERENCES public.orders(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: order_tax_lines fk_order_tax_lines_order_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_tax_lines
    ADD CONSTRAINT fk_order_tax_lines_order_id__id FOREIGN KEY (order_id) REFERENCES public.orders(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: order_tax_lines fk_order_tax_lines_tax_rate_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_tax_lines
    ADD CONSTRAINT fk_order_tax_lines_tax_rate_id__id FOREIGN KEY (tax_rate_id) REFERENCES public.tax_rates(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: orders fk_orders_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fk_orders_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: otp_codes fk_otp_codes_user_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.otp_codes
    ADD CONSTRAINT fk_otp_codes_user_id__id FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: payments fk_payments_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk_payments_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: products fk_products_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fk_products_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: refresh_tokens fk_refresh_tokens_user_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user_id__id FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: social_channels fk_social_channels_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.social_channels
    ADD CONSTRAINT fk_social_channels_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: social_conversations fk_social_conversations_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.social_conversations
    ADD CONSTRAINT fk_social_conversations_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: social_conversations fk_social_conversations_channel_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.social_conversations
    ADD CONSTRAINT fk_social_conversations_channel_id__id FOREIGN KEY (channel_id) REFERENCES public.social_channels(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: social_messages fk_social_messages_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.social_messages
    ADD CONSTRAINT fk_social_messages_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: social_messages fk_social_messages_conversation_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.social_messages
    ADD CONSTRAINT fk_social_messages_conversation_id__id FOREIGN KEY (conversation_id) REFERENCES public.social_conversations(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: social_orders fk_social_orders_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.social_orders
    ADD CONSTRAINT fk_social_orders_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: social_orders fk_social_orders_conversation_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.social_orders
    ADD CONSTRAINT fk_social_orders_conversation_id__id FOREIGN KEY (conversation_id) REFERENCES public.social_conversations(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: social_orders fk_social_orders_order_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.social_orders
    ADD CONSTRAINT fk_social_orders_order_id__id FOREIGN KEY (order_id) REFERENCES public.orders(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: stock_movements fk_stock_movements_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_movements
    ADD CONSTRAINT fk_stock_movements_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: stock_movements fk_stock_movements_product_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_movements
    ADD CONSTRAINT fk_stock_movements_product_id__id FOREIGN KEY (product_id) REFERENCES public.products(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tax_rates fk_tax_rates_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_rates
    ADD CONSTRAINT fk_tax_rates_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tax_remittances fk_tax_remittances_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_remittances
    ADD CONSTRAINT fk_tax_remittances_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tax_returns fk_tax_returns_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_returns
    ADD CONSTRAINT fk_tax_returns_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: users fk_users_business_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk_users_business_id__id FOREIGN KEY (business_id) REFERENCES public.businesses(id) ON UPDATE SET NULL ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

