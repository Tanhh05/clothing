package com.clothing.service.impl;

import com.clothing.dto.response.WishlistResponse;
import com.clothing.entity.UserEntity;
import com.clothing.entity.WishlistEntity;
import com.clothing.entity.WishlistItemEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.ProductRepository;
import com.clothing.repository.UserRepository;
import com.clothing.repository.WishlistItemRepository;
import com.clothing.repository.WishlistRepository;
import com.clothing.service.WishlistService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WishlistServiceImpl implements WishlistService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;

    public WishlistServiceImpl(
            UserRepository userRepository,
            ProductRepository productRepository,
            WishlistRepository wishlistRepository,
            WishlistItemRepository wishlistItemRepository
    ) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.wishlistRepository = wishlistRepository;
        this.wishlistItemRepository = wishlistItemRepository;
    }

    @Override
    public WishlistResponse getMyWishlist(String username) {
        UserEntity user = getUser(username);
        WishlistEntity wishlist = getOrCreateWishlist(user.getId());
        return buildResponse(wishlist, user.getId());
    }

    @Override
    @Transactional
    public WishlistResponse addItem(String username, Long productId) {
        UserEntity user = getUser(username);
        WishlistEntity wishlist = getOrCreateWishlist(user.getId());
        ensureProductExists(productId);

        boolean exists = wishlistItemRepository.findByWishlistIdAndProductId(wishlist.getId(), productId).isPresent();
        if (!exists) {
            WishlistItemEntity item = new WishlistItemEntity();
            item.setWishlistId(wishlist.getId());
            item.setProductId(productId);
            wishlistItemRepository.save(item);
        }

        return buildResponse(wishlist, user.getId());
    }

    @Override
    @Transactional
    public WishlistResponse removeItem(String username, Long productId) {
        UserEntity user = getUser(username);
        WishlistEntity wishlist = getOrCreateWishlist(user.getId());

        wishlistItemRepository.findByWishlistIdAndProductId(wishlist.getId(), productId)
                .ifPresent(wishlistItemRepository::delete);

        return buildResponse(wishlist, user.getId());
    }

    private WishlistResponse buildResponse(WishlistEntity wishlist, Long userId) {
        List<Long> productIds = wishlistItemRepository.findByWishlistIdOrderByIdAsc(wishlist.getId()).stream()
                .map(WishlistItemEntity::getProductId)
                .toList();

        return WishlistResponse.builder()
                .wishlistId(wishlist.getId())
                .userId(userId)
                .productIds(productIds)
                .build();
    }

    private WishlistEntity getOrCreateWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId).orElseGet(() -> {
            WishlistEntity wishlist = new WishlistEntity();
            wishlist.setUserId(userId);
            return wishlistRepository.save(wishlist);
        });
    }

    private UserEntity getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
    }

    private void ensureProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new BusinessException("Product not found", HttpStatus.BAD_REQUEST);
        }
    }
}
