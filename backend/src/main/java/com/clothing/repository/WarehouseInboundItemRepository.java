package com.clothing.repository;

import com.clothing.entity.WarehouseInboundItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseInboundItemRepository extends JpaRepository<WarehouseInboundItemEntity, Long> {

    List<WarehouseInboundItemEntity> findByInboundIdOrderByIdAsc(Long inboundId);
}
