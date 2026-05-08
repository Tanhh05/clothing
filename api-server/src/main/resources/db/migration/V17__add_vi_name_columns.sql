ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS name_vi VARCHAR(100);

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS name_vi VARCHAR(255),
    ADD COLUMN IF NOT EXISTS description_vi TEXT;

UPDATE categories
SET name_vi = COALESCE(NULLIF(TRIM(name_vi), ''), name);

UPDATE products
SET name_vi = COALESCE(NULLIF(TRIM(name_vi), ''), name),
    description_vi = COALESCE(NULLIF(TRIM(description_vi), ''), description);
