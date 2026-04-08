package com.clothing.repository;

import com.clothing.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByUserIdOrderByIdDesc(Long userId);

    List<NotificationEntity> findTop200ByOrderByIdDesc();

    List<NotificationEntity> findTop200ByUserIdIsNullOrderByIdDesc();

    List<NotificationEntity> findTop50ByUserIdOrderByIdDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);
}
