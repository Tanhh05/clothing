package com.clothing.repository;

import com.clothing.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {

    Optional<ProductEntity> findBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsByCategoryId(Long categoryId);

    List<ProductEntity> findAllByOrderByIdDesc();

    List<ProductEntity> findByDeletedTrueOrderByIdDesc();

    @Query("""
            select p
            from ProductEntity p
            where (p.deleted is null or p.deleted = false)
              and upper(coalesce(p.status, '')) = 'ACTIVE'
              and (
                lower(coalesce(p.name, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.slug, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.brand, '')) like lower(concat('%', :keyword, '%'))
              )
            order by p.id desc
            """)
    List<ProductEntity> searchActiveForFallback(@Param("keyword") String keyword, Pageable pageable);
}
