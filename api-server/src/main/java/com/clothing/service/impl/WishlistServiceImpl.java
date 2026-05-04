package com.clothing.service.impl;

import com.clothing.dto.response.WishlistResponse;
import com.clothing.dto.response.WishlistDealResponse;
import com.clothing.entity.NotificationEntity;
import com.clothing.entity.ProductEntity;
import com.clothing.entity.UserEntity;
import com.clothing.entity.WishlistEntity;
import com.clothing.entity.WishlistItemEntity;
import com.clothing.entity.WishlistPriceAlertEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.NotificationRepository;
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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class WishlistServiceImpl implements WishlistService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final WishlistPriceAlertRepository wishlistPriceAlertRepository;
    private final NotificationRepository notificationRepository;

    public WishlistServiceImpl(
            UserRepository userRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            WishlistRepository wishlistRepository,
            WishlistItemRepository wishlistItemRepository,
            WishlistPriceAlertRepository wishlistPriceAlertRepository,
            NotificationRepository notificationRepository
    ) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.wishlistRepository = wishlistRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.wishlistPriceAlertRepository = wishlistPriceAlertRepository;
        this.notificationRepository = notificationRepository;
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
                .ifPresent(item -> {
                    wishlistItemRepository.delete(item);
                    wishlistPriceAlertRepository.deleteByUserIdAndProductId(user.getId(), productId);
                });

        return buildResponse(wishlist, user.getId());
    }

    @Override
    @Transactional
    public void upsertPriceAlert(String username, Long productId, Long targetPrice) {
        UserEntity user = getUser(username);
        WishlistEntity wishlist = getOrCreateWishlist(user.getId());
        ensureProductExists(productId);
        boolean wished = wishlistItemRepository.findByWishlistIdAndProductId(wishlist.getId(), productId).isPresent();
        if (!wished) {
            throw new BusinessException("Product is not in wishlist", HttpStatus.BAD_REQUEST);
        }
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
    @Transactional
    public void notifyPriceDrop(Long productId, Long oldMinPrice, Long newMinPrice) {
        if (productId == null) return;
        long oldPrice = oldMinPrice == null ? 0L : oldMinPrice;
        long newPrice = newMinPrice == null ? 0L : newMinPrice;
        if (newPrice <= 0 || oldPrice <= newPrice) return;

        ProductEntity product = productRepository.findById(productId).orElse(null);
        if (product == null || Boolean.TRUE.equals(product.getDeleted())) return;

        List<Long> userIds = wishlistRepository.findDistinctUserIdsByProductId(productId);
        if (userIds.isEmpty()) return;

        Map<Long, Long> alertTargetsByUser = new HashMap<>();
        wishlistPriceAlertRepository.findByProductIdAndUserIdInOrderByIdDesc(productId, userIds)
                .forEach(alert -> {
                    if (!alertTargetsByUser.containsKey(alert.getUserId())) {
                        alertTargetsByUser.put(alert.getUserId(), Math.max(0L, alert.getTargetPrice() == null ? 0L : alert.getTargetPrice()));
                    }
                });

        LocalDateTime now = LocalDateTime.now();
        List<NotificationEntity> notifications = userIds.stream()
                .filter(userId -> shouldNotify(alertTargetsByUser.get(userId), oldPrice, newPrice))
                .map(userId -> {
                    Long target = alertTargetsByUser.getOrDefault(userId, 0L);
                    NotificationEntity entity = new NotificationEntity();
                    entity.setUserId(userId);
                    entity.setTitle("Sản phẩm yêu thích vừa giảm giá");
                    entity.setContent(buildPriceDropContent(product, oldPrice, newPrice, target));
                    entity.setType("WISHLIST_PRICE_DROP");
                    entity.setAudience("USER");
                    entity.setChannel("IN_APP");
                    entity.setStatus("SENT");
                    entity.setScheduledAt(null);
                    entity.setIsRead(false);
                    entity.setCreatedAt(now);
                    return entity;
                })
                .toList();

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
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
        Set<Long> productIdSet = new HashSet<>(productIds);
        Map<Long, Long> priceAlertTargets = wishlistPriceAlertRepository.findByUserIdOrderByIdDesc(userId).stream()
                .filter(item -> item.getProductId() != null && productIdSet.contains(item.getProductId()))
                .collect(
                        HashMap::new,
                        (map, item) -> map.putIfAbsent(item.getProductId(), Math.max(0L, item.getTargetPrice() == null ? 0L : item.getTargetPrice())),
                        HashMap::putAll
                );

        return WishlistResponse.builder()
                .wishlistId(wishlist.getId())
                .userId(userId)
                .productIds(productIds)
                .priceAlertTargets(priceAlertTargets)
                .build();
    }

    private boolean shouldNotify(Long targetPrice, long oldPrice, long newPrice) {
        long target = targetPrice == null ? 0L : Math.max(0L, targetPrice);
        if (target <= 0L) {
            return true;
        }
        return oldPrice > target && newPrice <= target;
    }

    private String buildPriceDropContent(ProductEntity product, long oldPrice, long newPrice, Long targetPrice) {
        String slugOrId = product.getSlug() == null || product.getSlug().isBlank()
                ? String.valueOf(product.getId())
                : product.getSlug();
        StringBuilder builder = new StringBuilder();
        builder.append("“")
                .append(product.getName())
                .append("” giảm từ ")
                .append(formatPrice(oldPrice))
                .append(" còn ")
                .append(formatPrice(newPrice));
        long target = targetPrice == null ? 0L : Math.max(0L, targetPrice);
        if (target > 0L) {
            builder.append(" (mục tiêu ").append(formatPrice(target)).append(")");
        }
        builder.append(". Xem ngay: /products/").append(slugOrId);
        return builder.toString();
    }

    private String formatPrice(long price) {
        return String.format("%,d ₫", Math.max(0L, price)).replace(',', '.');
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
