package com.clothing.repository;

import com.clothing.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {

    List<CartItemEntity> findByCartIdOrderByIdAsc(Long cartId);

    Optional<CartItemEntity> findByCartIdAndVariantId(Long cartId, Long variantId);

    void deleteByCartId(Long cartId);
}
