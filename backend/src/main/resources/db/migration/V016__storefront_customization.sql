ALTER TABLE businesses ADD COLUMN storefront_theme_color VARCHAR(7) NOT NULL DEFAULT '#0F766E';
ALTER TABLE businesses ADD COLUMN storefront_headline VARCHAR(120) NOT NULL DEFAULT 'Shop with us online';
ALTER TABLE businesses ADD COLUMN storefront_description VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE businesses ADD COLUMN storefront_banner_url TEXT;
ALTER TABLE businesses ADD COLUMN storefront_layout VARCHAR(20) NOT NULL DEFAULT 'GRID';
