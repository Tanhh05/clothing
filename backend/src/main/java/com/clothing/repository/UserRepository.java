package com.clothing.repository;

import com.clothing.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("select distinct u.id from UserEntity u join u.roles r where upper(r.name) = 'USER'")
    List<Long> findCustomerUserIds();

    @Query("select distinct u.id from UserEntity u join u.roles r " +
            "where upper(r.name) = 'USER' and (u.status is null or upper(u.status) = 'ACTIVE')")
    List<Long> findActiveCustomerUserIds();
}
