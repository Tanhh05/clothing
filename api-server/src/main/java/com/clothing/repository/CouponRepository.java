package com.clothing.repository;

import com.clothing.entity.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<CouponEntity, Long> {

    List<CouponEntity> findAllByOrderByIdDesc();

    Optional<CouponEntity> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
