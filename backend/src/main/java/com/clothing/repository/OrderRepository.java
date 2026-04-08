package com.clothing.repository;

import com.clothing.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long>, JpaSpecificationExecutor<OrderEntity> {

    List<OrderEntity> findByUserIdOrderByIdDesc(Long userId);

    List<OrderEntity> findAllByOrderByIdDesc();

    @Query("select distinct o.userId from OrderEntity o where o.userId is not null")
    List<Long> findDistinctUserIds();
}
