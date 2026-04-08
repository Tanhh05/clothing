package com.clothing.repository;

import com.clothing.entity.InventoryLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryLogRepository extends JpaRepository<InventoryLogEntity, Long> {

    List<InventoryLogEntity> findTop200ByOrderByIdDesc();

    List<InventoryLogEntity> findTop100ByVariantIdOrderByIdDesc(Long variantId);
}
