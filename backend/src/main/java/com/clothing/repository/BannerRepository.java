package com.clothing.repository;

import com.clothing.entity.BannerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BannerRepository extends JpaRepository<BannerEntity, Long> {

    @Query("select b from BannerEntity b where coalesce(b.deleted, false) = false order by b.id desc")
    List<BannerEntity> findAllVisibleOrderByIdDesc();

    @Query("""
            select b from BannerEntity b
            where coalesce(b.deleted, false) = false
              and b.status = :status
            order by b.id desc
            """)
    List<BannerEntity> findAllVisibleByStatusOrderByIdDesc(String status);
}
