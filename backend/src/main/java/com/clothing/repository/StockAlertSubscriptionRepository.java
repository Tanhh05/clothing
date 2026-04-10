package com.clothing.repository;

import com.clothing.entity.StockAlertSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockAlertSubscriptionRepository extends JpaRepository<StockAlertSubscriptionEntity, Long> {

    Optional<StockAlertSubscriptionEntity> findByUserIdAndProductIdAndSizeAndColor(
            Long userId,
            Long productId,
            String size,
            String color
    );

    List<StockAlertSubscriptionEntity> findByProductIdAndNotifiedFalse(Long productId);
}
