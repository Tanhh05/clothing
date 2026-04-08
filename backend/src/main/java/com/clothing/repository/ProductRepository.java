package com.clothing.repository;

import com.clothing.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {

    Optional<ProductEntity> findBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsByCategoryId(Long categoryId);

    List<ProductEntity> findAllByOrderByIdDesc();

    List<ProductEntity> findByDeletedTrueOrderByIdDesc();
}
