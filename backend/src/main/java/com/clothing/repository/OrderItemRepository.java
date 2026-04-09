package com.clothing.repository;

import com.clothing.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    List<OrderItemEntity> findByOrderIdOrderByIdAsc(Long orderId);

    List<OrderItemEntity> findByIdIn(Set<Long> ids);
}
