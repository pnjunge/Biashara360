-- Store bounded category thumbnails as data URLs alongside existing external image URLs.
ALTER TABLE inventory_categories ALTER COLUMN image_url TYPE TEXT;
