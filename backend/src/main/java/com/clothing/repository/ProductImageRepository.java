package com.clothing.repository;

import com.clothing.entity.ProductImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImageEntity, Long> {

    List<ProductImageEntity> findByProductIdOrderByIdAsc(Long productId);

    void deleteByProductId(Long productId);
}
