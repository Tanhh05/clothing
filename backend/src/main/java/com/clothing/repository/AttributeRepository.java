package com.clothing.repository;

import com.clothing.entity.AttributeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttributeRepository extends JpaRepository<AttributeEntity, Long> {

    Optional<AttributeEntity> findByNameIgnoreCase(String name);
}
