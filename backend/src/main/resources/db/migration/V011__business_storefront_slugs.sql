ALTER TABLE businesses ADD COLUMN storefront_slug VARCHAR(120);

WITH normalized AS (
    SELECT
        id,
        LEFT(
            TRIM(BOTH '-' FROM REGEXP_REPLACE(
                REGEXP_REPLACE(LOWER(name), '[''’]', '', 'g'),
                '[^a-z0-9]+', '-', 'g'
            )),
            100
        ) AS base_slug
    FROM businesses
), ranked AS (
    SELECT
        id,
        CASE WHEN base_slug = '' THEN 'business' ELSE base_slug END AS base_slug,
        ROW_NUMBER() OVER (
            PARTITION BY CASE WHEN base_slug = '' THEN 'business' ELSE base_slug END
            ORDER BY id
        ) AS duplicate_number
    FROM normalized
)
UPDATE businesses AS business
SET storefront_slug = CASE
    WHEN ranked.duplicate_number = 1 THEN ranked.base_slug
    ELSE ranked.base_slug || '-' || ranked.duplicate_number
END
FROM ranked
WHERE business.id = ranked.id;

ALTER TABLE businesses ALTER COLUMN storefront_slug SET NOT NULL;
CREATE UNIQUE INDEX idx_businesses_storefront_slug ON businesses (storefront_slug);
