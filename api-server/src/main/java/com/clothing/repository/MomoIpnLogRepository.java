package com.clothing.repository;

import com.clothing.entity.MomoIpnLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MomoIpnLogRepository extends JpaRepository<MomoIpnLogEntity, Long> {

    List<MomoIpnLogEntity> findTop200ByOrderByIdDesc();

    List<MomoIpnLogEntity> findTop200ByOrderIdOrderByIdDesc(String orderId);
}
