UPDATE products
SET
    name_vi = CASE slug
        WHEN 'cotton-t-shirt' THEN 'Áo thun cotton'
        WHEN 'jeans' THEN 'Quần jean'
        WHEN 'summer-dress' THEN 'Váy mùa hè'
        WHEN 'sneakerssss' THEN 'Giày thể thao'
        WHEN 'test' THEN 'Sản phẩm thử nghiệm'
        ELSE COALESCE(name_vi, name)
    END,
    description_vi = CASE slug
        WHEN 'cotton-t-shirt' THEN 'Áo thun cotton thoải mái'
        WHEN 'jeans' THEN 'Quần jean xanh denim'
        WHEN 'summer-dress' THEN 'Váy mùa hè nhẹ nhàng'
        WHEN 'sneakerssss' THEN 'Giày thể thao êm chân'
        WHEN 'test' THEN 'Mô tả thử nghiệm'
        ELSE COALESCE(description_vi, description)
    END
WHERE is_deleted = FALSE;
