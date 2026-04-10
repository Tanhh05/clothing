package com.clothing.repository;

import com.clothing.entity.WishlistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<WishlistEntity, Long> {

    Optional<WishlistEntity> findByUserId(Long userId);

    @Query("select distinct w.userId from WishlistEntity w where exists (select 1 from WishlistItemEntity wi where wi.wishlistId = w.id)")
    List<Long> findDistinctUserIdsWithItems();

    @Query("select distinct w.userId from WishlistEntity w where exists (select 1 from WishlistItemEntity wi where wi.wishlistId = w.id and wi.productId = :productId)")
    List<Long> findDistinctUserIdsByProductId(@Param("productId") Long productId);
}
