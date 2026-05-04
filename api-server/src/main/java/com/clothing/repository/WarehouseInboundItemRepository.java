package com.clothing.repository;

import com.clothing.entity.WarehouseInboundItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WarehouseInboundItemRepository extends JpaRepository<WarehouseInboundItemEntity, Long> {

    List<WarehouseInboundItemEntity> findByInboundIdOrderByIdAsc(Long inboundId);

    @Query("""
            select i.inboundId as inboundId, coalesce(sum(i.quantity), 0) as totalQuantity
            from WarehouseInboundItemEntity i
            where i.inboundId in :inboundIds
            group by i.inboundId
            """)
    List<InboundQuantityProjection> sumQuantityByInboundIds(@Param("inboundIds") List<Long> inboundIds);
}
