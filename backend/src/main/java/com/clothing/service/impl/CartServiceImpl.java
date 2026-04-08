package com.clothing.service.impl;

import com.clothing.dto.request.AddCartItemRequest;
import com.clothing.dto.request.UpdateCartItemRequest;
import com.clothing.dto.response.CartItemResponse;
import com.clothing.dto.response.CartResponse;
import com.clothing.entity.CartEntity;
import com.clothing.entity.CartItemEntity;
import com.clothing.entity.ProductVariantEntity;
import com.clothing.entity.UserEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.CartItemRepository;
import com.clothing.repository.CartRepository;
import com.clothing.repository.ProductVariantRepository;
import com.clothing.repository.UserRepository;
import com.clothing.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;

    public CartServiceImpl(
            UserRepository userRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductVariantRepository productVariantRepository
    ) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
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
            long lineTotal = variant.getPrice() * item.getQuantity();
            return CartItemResponse.builder()
                    .id(item.getId())
                    .variantId(item.getVariantId())
                    .sku(variant.getSku())
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
}
