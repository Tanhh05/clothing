package com.clothing.repository;

import com.clothing.entity.PasswordResetOtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtpEntity, Long> {

    Optional<PasswordResetOtpEntity> findTopByEmailOrderByCreatedAtDesc(String email);
}
