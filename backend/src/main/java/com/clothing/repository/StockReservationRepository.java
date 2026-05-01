package com.clothing.repository;

import com.clothing.entity.StockReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockReservationRepository extends JpaRepository<StockReservationEntity, Long> {

    @Query("""
            select coalesce(sum(r.quantity), 0)
            from StockReservationEntity r
            where r.variantId = :variantId
              and upper(r.status) = upper(:status)
              and r.expiresAt > :now
            """)
    long sumActiveQuantityByVariantId(
            @Param("variantId") Long variantId,
            @Param("status") String status,
            @Param("now") LocalDateTime now
    );

    boolean existsByOrderIdAndStatusIgnoreCase(Long orderId, String status);

    List<StockReservationEntity> findByOrderIdAndStatusIgnoreCaseOrderByIdAsc(Long orderId, String status);
}
