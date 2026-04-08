package com.clothing.repository;

import com.clothing.entity.VariantAttributeValueEntity;
import com.clothing.entity.VariantAttributeValueId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VariantAttributeValueRepository extends JpaRepository<VariantAttributeValueEntity, VariantAttributeValueId> {

    List<VariantAttributeValueEntity> findByVariantId(Long variantId);

    void deleteByVariantId(Long variantId);
}
