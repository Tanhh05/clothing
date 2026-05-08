ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS name_my VARCHAR(100);

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS name_my VARCHAR(255),
    ADD COLUMN IF NOT EXISTS description_my TEXT;

UPDATE categories
SET name_my = COALESCE(NULLIF(TRIM(name_my), ''), NULLIF(TRIM(name_vi), ''), NULLIF(TRIM(name_en), ''), name);

UPDATE products
SET name_my = COALESCE(NULLIF(TRIM(name_my), ''), NULLIF(TRIM(name_vi), ''), NULLIF(TRIM(name_en), ''), name),
    description_my = COALESCE(NULLIF(TRIM(description_my), ''), NULLIF(TRIM(description_vi), ''), NULLIF(TRIM(description_en), ''), description);
