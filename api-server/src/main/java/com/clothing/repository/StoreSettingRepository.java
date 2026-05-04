package com.clothing.repository;

import com.clothing.entity.StoreSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreSettingRepository extends JpaRepository<StoreSettingEntity, Long> {

    Optional<StoreSettingEntity> findBySettingKey(String settingKey);

    List<StoreSettingEntity> findAllByOrderBySettingKeyAsc();
}
