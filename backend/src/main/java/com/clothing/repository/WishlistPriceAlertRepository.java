package com.clothing.repository;

import com.clothing.entity.WishlistPriceAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistPriceAlertRepository extends JpaRepository<WishlistPriceAlertEntity, Long> {

    Optional<WishlistPriceAlertEntity> findByUserIdAndProductId(Long userId, Long productId);

    List<WishlistPriceAlertEntity> findByUserIdOrderByIdDesc(Long userId);

    List<WishlistPriceAlertEntity> findByProductIdAndUserIdInOrderByIdDesc(Long productId, List<Long> userIds);

    void deleteByUserIdAndProductId(Long userId, Long productId);
}
