package com.clothing.service.impl;

import com.clothing.dto.request.AddCartItemRequest;
import com.clothing.dto.request.UpdateCartItemRequest;
import com.clothing.dto.response.CartItemResponse;
import com.clothing.dto.response.CartResponse;
import com.clothing.entity.CartEntity;
import com.clothing.entity.CartItemEntity;
import com.clothing.entity.ProductEntity;
import com.clothing.entity.ProductImageEntity;
import com.clothing.entity.ProductVariantEntity;
import com.clothing.entity.AttributeEntity;
import com.clothing.entity.AttributeValueEntity;
import com.clothing.entity.VariantAttributeValueEntity;
import com.clothing.entity.UserEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.AttributeRepository;
import com.clothing.repository.AttributeValueRepository;
import com.clothing.repository.CartItemRepository;
import com.clothing.repository.CartRepository;
import com.clothing.repository.ProductImageRepository;
import com.clothing.repository.ProductRepository;
import com.clothing.repository.ProductVariantRepository;
import com.clothing.repository.UserRepository;
import com.clothing.repository.VariantAttributeValueRepository;
import com.clothing.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final VariantAttributeValueRepository variantAttributeValueRepository;
    private final AttributeValueRepository attributeValueRepository;
    private final AttributeRepository attributeRepository;

    public CartServiceImpl(
            UserRepository userRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductVariantRepository productVariantRepository,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            VariantAttributeValueRepository variantAttributeValueRepository,
            AttributeValueRepository attributeValueRepository,
            AttributeRepository attributeRepository
    ) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.variantAttributeValueRepository = variantAttributeValueRepository;
        this.attributeValueRepository = attributeValueRepository;
        this.attributeRepository = attributeRepository;
    }

    @Override
    public CartResponse getMyCart(String username) {
        UserEntity user = getUser(username);
        CartEntity cart = getOrCreateCart(user.getId());
        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(String username, AddCartItemRequest request) {
        UserEntity user = getUser(username);
        CartEntity cart = getOrCreateCart(user.getId());
        ProductVariantEntity variant = getVariant(request.getVariantId());
        ensureStock(variant, request.getQuantity());

        CartItemEntity item = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId())
                .orElseGet(() -> {
                    CartItemEntity newItem = new CartItemEntity();
                    newItem.setCartId(cart.getId());
                    newItem.setVariantId(variant.getId());
                    newItem.setQuantity(0);
                    return newItem;
                });
        item.setQuantity(item.getQuantity() + request.getQuantity());
        ensureStock(variant, item.getQuantity());
        cartItemRepository.save(item);

        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItem(String username, Long cartItemId, UpdateCartItemRequest request) {
        UserEntity user = getUser(username);
        CartEntity cart = getOrCreateCart(user.getId());

        CartItemEntity item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException("Cart item not found", HttpStatus.NOT_FOUND));
        if (!item.getCartId().equals(cart.getId())) {
            throw new BusinessException("Cart item does not belong to current user", HttpStatus.FORBIDDEN);
        }

        ProductVariantEntity variant = getVariant(item.getVariantId());
        ensureStock(variant, request.getQuantity());
        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(String username, Long cartItemId) {
        UserEntity user = getUser(username);
        CartEntity cart = getOrCreateCart(user.getId());

        CartItemEntity item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException("Cart item not found", HttpStatus.NOT_FOUND));
        if (!item.getCartId().equals(cart.getId())) {
            throw new BusinessException("Cart item does not belong to current user", HttpStatus.FORBIDDEN);
        }
        cartItemRepository.delete(item);
        return buildCartResponse(cart);
    }

    private CartResponse buildCartResponse(CartEntity cart) {
        List<CartItemEntity> items = cartItemRepository.findByCartIdOrderByIdAsc(cart.getId());
        List<CartItemResponse> responses = items.stream().map(item -> {
            ProductVariantEntity variant = getVariant(item.getVariantId());
            ProductEntity product = productRepository.findById(variant.getProductId()).orElse(null);
            Map<String, String> attributes = resolveVariantAttributes(variant.getId());
            String image = resolveProductImage(variant.getProductId());
            long lineTotal = variant.getPrice() * item.getQuantity();
            return CartItemResponse.builder()
                    .id(item.getId())
                    .productId(variant.getProductId())
                    .productSlug(product == null ? null : product.getSlug())
                    .productName(product == null ? variant.getSku() : product.getName())
                    .productImage(image)
                    .variantId(item.getVariantId())
                    .sku(variant.getSku())
                    .size(attributes.get("size"))
                    .color(attributes.get("color"))
                    .price(variant.getPrice())
                    .quantity(item.getQuantity())
                    .lineTotal(lineTotal)
                    .build();
        }).toList();

        long total = responses.stream().mapToLong(CartItemResponse::getLineTotal).sum();
        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUserId())
                .items(responses)
                .totalPrice(total)
                .build();
    }

    private UserEntity getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
    }

    private CartEntity getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    CartEntity cart = new CartEntity();
                    cart.setUserId(userId);
                    cart.setCreatedAt(LocalDateTime.now());
                    return cartRepository.save(cart);
                });
    }

    private ProductVariantEntity getVariant(Long variantId) {
        return productVariantRepository.findById(variantId)
                .orElseThrow(() -> new BusinessException("Variant not found", HttpStatus.BAD_REQUEST));
    }

    private void ensureStock(ProductVariantEntity variant, Integer quantity) {
        if (quantity > variant.getStock()) {
            throw new BusinessException("Not enough stock for variant: " + variant.getSku(), HttpStatus.BAD_REQUEST);
        }
    }

    private Map<String, String> resolveVariantAttributes(Long variantId) {
        String size = "";
        String color = "";
        List<VariantAttributeValueEntity> links = variantAttributeValueRepository.findByVariantId(variantId);
        for (VariantAttributeValueEntity link : links) {
            AttributeValueEntity value = attributeValueRepository.findById(link.getAttributeValueId()).orElse(null);
            if (value == null || value.getAttributeId() == null) {
                continue;
            }
            AttributeEntity attribute = attributeRepository.findById(value.getAttributeId()).orElse(null);
            if (attribute == null || attribute.getName() == null) {
                continue;
            }
            String attrName = attribute.getName().trim().toLowerCase(Locale.ROOT);
            if ("size".equals(attrName)) {
                size = value.getValue() == null ? "" : value.getValue();
            } else if ("color".equals(attrName)) {
                color = value.getValue() == null ? "" : value.getValue();
            }
        }
        return Map.of("size", size, "color", color);
    }

    private String resolveProductImage(Long productId) {
        List<ProductImageEntity> images = productImageRepository.findByProductIdOrderByIdAsc(productId);
        for (ProductImageEntity image : images) {
            if (Boolean.TRUE.equals(image.getIsMain()) && image.getUrl() != null && !image.getUrl().isBlank()) {
                return image.getUrl();
            }
        }
        return images.stream()
                .map(ProductImageEntity::getUrl)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse("");
    }
}
