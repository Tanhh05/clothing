package com.clothing.repository;

import com.clothing.entity.VnpayCheckoutSessionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VnpayCheckoutSessionRepository extends JpaRepository<VnpayCheckoutSessionEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from VnpayCheckoutSessionEntity s where s.token = :token")
    Optional<VnpayCheckoutSessionEntity> findByTokenForUpdate(@Param("token") String token);
}
