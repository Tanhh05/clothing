package com.clothing.repository;

import com.clothing.entity.UserAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddressEntity, Long> {

    List<UserAddressEntity> findByUserIdOrderByIdDesc(Long userId);

    Optional<UserAddressEntity> findByUserIdAndIsDefaultTrue(Long userId);
}
