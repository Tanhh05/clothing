package com.clothing.repository;

import com.clothing.entity.ReturnRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequestEntity, Long> {

    List<ReturnRequestEntity> findAllByOrderByIdDesc();

    List<ReturnRequestEntity> findByStatusOrderByIdDesc(String status);

    List<ReturnRequestEntity> findByUserIdOrderByIdDesc(Long userId);

    List<ReturnRequestEntity> findByUserIdAndStatusOrderByIdDesc(Long userId, String status);

    boolean existsByOrderIdAndStatusIn(Long orderId, Set<String> statuses);
}
