ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS name_en VARCHAR(100);

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS name_en VARCHAR(255),
    ADD COLUMN IF NOT EXISTS description_en TEXT;

UPDATE categories
SET name_en = COALESCE(NULLIF(TRIM(name_en), ''), name);

UPDATE products
SET name_en = COALESCE(NULLIF(TRIM(name_en), ''), name),
    description_en = COALESCE(NULLIF(TRIM(description_en), ''), description);
