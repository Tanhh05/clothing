package com.clothing.repository;

import com.clothing.entity.WishlistItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItemEntity, Long> {

    List<WishlistItemEntity> findByWishlistIdOrderByIdAsc(Long wishlistId);

    Optional<WishlistItemEntity> findByWishlistIdAndProductId(Long wishlistId, Long productId);
}
