package com.clothing.service.impl;

import com.clothing.dto.response.WishlistResponse;
import com.clothing.dto.response.WishlistDealResponse;
import com.clothing.entity.ProductEntity;
import com.clothing.entity.UserEntity;
import com.clothing.entity.WishlistEntity;
import com.clothing.entity.WishlistItemEntity;
import com.clothing.entity.WishlistPriceAlertEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.ProductRepository;
import com.clothing.repository.ProductVariantRepository;
import com.clothing.repository.UserRepository;
import com.clothing.repository.WishlistPriceAlertRepository;
import com.clothing.repository.WishlistItemRepository;
import com.clothing.repository.WishlistRepository;
import com.clothing.service.WishlistService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class WishlistServiceImpl implements WishlistService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final WishlistPriceAlertRepository wishlistPriceAlertRepository;

    public WishlistServiceImpl(
            UserRepository userRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            WishlistRepository wishlistRepository,
            WishlistItemRepository wishlistItemRepository,
            WishlistPriceAlertRepository wishlistPriceAlertRepository
    ) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.wishlistRepository = wishlistRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.wishlistPriceAlertRepository = wishlistPriceAlertRepository;
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

    @Override
    @Transactional
    public void upsertPriceAlert(String username, Long productId, Long targetPrice) {
        UserEntity user = getUser(username);
        ensureProductExists(productId);
        long normalizedTarget = targetPrice == null ? 0L : Math.max(0L, targetPrice);
        WishlistPriceAlertEntity entity = wishlistPriceAlertRepository.findByUserIdAndProductId(user.getId(), productId)
                .orElseGet(() -> {
                    WishlistPriceAlertEntity created = new WishlistPriceAlertEntity();
                    created.setUserId(user.getId());
                    created.setProductId(productId);
                    created.setCreatedAt(java.time.LocalDateTime.now());
                    return created;
                });
        entity.setTargetPrice(normalizedTarget);
        wishlistPriceAlertRepository.save(entity);
    }

    @Override
    public List<WishlistDealResponse> getDeals(String username) {
        UserEntity user = getUser(username);
        List<WishlistPriceAlertEntity> alerts = wishlistPriceAlertRepository.findByUserIdOrderByIdDesc(user.getId());
        return alerts.stream()
                .map(alert -> {
                    ProductEntity product = productRepository.findById(alert.getProductId()).orElse(null);
                    if (product == null || Boolean.TRUE.equals(product.getDeleted())) return null;
                    Long minPrice = productVariantRepository.findByProductIdOrderByIdAsc(product.getId()).stream()
                            .map(v -> v.getPrice() == null ? 0L : v.getPrice())
                            .min(Long::compareTo)
                            .orElse(0L);
                    Long target = alert.getTargetPrice() == null ? 0L : alert.getTargetPrice();
                    if (target <= 0 || minPrice > target) return null;
                    return WishlistDealResponse.builder()
                            .productId(product.getId())
                            .productName(product.getName())
                            .productSlug(product.getSlug())
                            .currentMinPrice(minPrice)
                            .targetPrice(target)
                            .diff(Math.max(0L, target - minPrice))
                            .build();
                })
                .filter(item -> item != null)
                .toList();
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
