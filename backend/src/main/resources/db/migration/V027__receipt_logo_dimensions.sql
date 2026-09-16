ALTER TABLE businesses
    ADD COLUMN receipt_logo_width_mm INTEGER NOT NULL DEFAULT 42,
    ADD COLUMN receipt_logo_height_mm INTEGER NOT NULL DEFAULT 20;
