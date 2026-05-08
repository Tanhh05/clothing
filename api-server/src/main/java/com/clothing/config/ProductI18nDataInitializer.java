package com.clothing.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductI18nDataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public ProductI18nDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.update("""
            UPDATE products
            SET
                name_vi = CASE
                    WHEN slug = 'cotton-t-shirt' THEN 'Áo thun cotton'
                    WHEN slug = 'jeans' THEN 'Quần jean'
                    WHEN slug = 'summer-dress' THEN 'Váy mùa hè'
                    WHEN slug = 'sneakerssss' THEN 'Giày thể thao'
                    WHEN slug = 'test' THEN 'Sản phẩm thử nghiệm'
                    ELSE name_vi
                END,
                description_vi = CASE
                    WHEN slug = 'cotton-t-shirt' THEN 'Áo thun cotton thoải mái'
                    WHEN slug = 'jeans' THEN 'Quần jean xanh denim'
                    WHEN slug = 'summer-dress' THEN 'Váy mùa hè nhẹ nhàng'
                    WHEN slug = 'sneakerssss' THEN 'Giày thể thao êm chân'
                    WHEN slug = 'test' THEN 'Mô tả thử nghiệm'
                    ELSE description_vi
                END
            WHERE slug IN ('cotton-t-shirt', 'jeans', 'summer-dress', 'sneakerssss', 'test')
            """);
    }
}
