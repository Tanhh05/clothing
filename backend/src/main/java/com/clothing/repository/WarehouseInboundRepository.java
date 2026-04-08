package com.clothing.repository;

import com.clothing.entity.WarehouseInboundEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseInboundRepository extends JpaRepository<WarehouseInboundEntity, Long> {

    List<WarehouseInboundEntity> findAllByOrderByIdDesc();

    Optional<WarehouseInboundEntity> findByCodeIgnoreCase(String code);
}
