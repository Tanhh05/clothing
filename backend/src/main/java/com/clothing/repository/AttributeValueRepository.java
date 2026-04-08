package com.clothing.repository;

import com.clothing.entity.AttributeValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttributeValueRepository extends JpaRepository<AttributeValueEntity, Long> {

    Optional<AttributeValueEntity> findByAttributeIdAndValueIgnoreCase(Long attributeId, String value);

    List<AttributeValueEntity> findByAttributeIdOrderByValueAsc(Long attributeId);
}
