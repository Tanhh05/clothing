package com.clothing.repository;

import com.clothing.entity.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, Long> {

    List<ProductVariantEntity> findByProductIdOrderByIdAsc(Long productId);

    List<ProductVariantEntity> findByProductIdInOrderByProductIdAscIdAsc(List<Long> productIds);

    void deleteByProductId(Long productId);

    boolean existsBySkuIgnoreCase(String sku);

    Optional<ProductVariantEntity> findBySkuIgnoreCase(String sku);

    List<ProductVariantEntity> findBySkuIn(List<String> skus);

    @Query("select v from ProductVariantEntity v where lower(v.sku) in :skusLower")
    List<ProductVariantEntity> findBySkuLowerIn(@Param("skusLower") List<String> skusLower);

    List<ProductVariantEntity> findTop200BySkuContainingIgnoreCaseOrderBySkuAsc(String q);

    List<ProductVariantEntity> findByStockLessThanEqualOrderByStockAsc(Integer stock);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariantEntity v where v.id = :id")
    Optional<ProductVariantEntity> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariantEntity v where lower(v.sku) = lower(:sku)")
    Optional<ProductVariantEntity> findBySkuIgnoreCaseForUpdate(@Param("sku") String sku);
}
