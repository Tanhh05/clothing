package com.clothing.repository;

import com.clothing.entity.ReturnRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequestEntity, Long> {

    List<ReturnRequestEntity> findAllByOrderByIdDesc();

    List<ReturnRequestEntity> findByStatusOrderByIdDesc(String status);
}
