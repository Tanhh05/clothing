package com.clothing.repository;

import com.clothing.entity.PosDraftEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PosDraftRepository extends JpaRepository<PosDraftEntity, Long> {

    Optional<PosDraftEntity> findByAdminUserIdAndTerminalId(Long adminUserId, String terminalId);

    void deleteByAdminUserIdAndTerminalId(Long adminUserId, String terminalId);
}

