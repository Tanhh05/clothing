package com.clothing.repository;

import com.clothing.entity.OrderStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistoryEntity, Long> {

    List<OrderStatusHistoryEntity> findByOrderIdOrderByIdAsc(Long orderId);

    Optional<OrderStatusHistoryEntity> findTopByOrderIdAndStatusOrderByChangedAtDesc(Long orderId, String status);
}
