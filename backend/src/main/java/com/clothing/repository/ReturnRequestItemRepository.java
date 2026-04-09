package com.clothing.repository;

import com.clothing.entity.ReturnRequestItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnRequestItemRepository extends JpaRepository<ReturnRequestItemEntity, Long> {

    List<ReturnRequestItemEntity> findByReturnRequestIdOrderByIdAsc(Long returnRequestId);
}
