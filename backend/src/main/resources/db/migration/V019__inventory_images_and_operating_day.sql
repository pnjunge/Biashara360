ALTER TABLE inventory_categories ADD COLUMN image_url VARCHAR(500);
ALTER TABLE businesses ADD COLUMN day_start_time VARCHAR(5) NOT NULL DEFAULT '06:00';
ALTER TABLE businesses ADD COLUMN day_close_time VARCHAR(5) NOT NULL DEFAULT '23:00';

ALTER TABLE businesses ADD CONSTRAINT chk_business_day_start_time
    CHECK (day_start_time ~ '^(?:[01][0-9]|2[0-3]):[0-5][0-9]$');
ALTER TABLE businesses ADD CONSTRAINT chk_business_day_close_time
    CHECK (day_close_time ~ '^(?:[01][0-9]|2[0-3]):[0-5][0-9]$');
ALTER TABLE businesses ADD CONSTRAINT chk_business_operating_times_differ
    CHECK (day_start_time <> day_close_time);
