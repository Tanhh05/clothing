package com.clothing.repository;

import com.clothing.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    long countByProductId(Long productId);

    @Query("select avg(r.rating) from ReviewEntity r where r.productId = :productId")
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    List<ReviewEntity> findByProductIdOrderByIdDesc(Long productId);

    boolean existsByUserIdAndOrderIdAndProductId(Long userId, Long orderId, Long productId);
}
