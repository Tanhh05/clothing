package com.clothing.repository;

import com.clothing.entity.CouponUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsageRepository extends JpaRepository<CouponUsageEntity, Long> {

    boolean existsByCouponIdAndUserId(Long couponId, Long userId);
}
