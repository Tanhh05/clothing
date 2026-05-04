package com.clothing.repository;

import com.clothing.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    Optional<CategoryEntity> findBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCase(String slug);

    List<CategoryEntity> findAllByOrderByIdAsc();

    boolean existsByParent_Id(Long parentId);

    Page<CategoryEntity> findByNameContainingIgnoreCaseOrSlugContainingIgnoreCaseOrShortContentContainingIgnoreCase(
            String nameKeyword,
            String slugKeyword,
            String shortContentKeyword,
            Pageable pageable
    );
}
