package com.clothing.controller;

import com.clothing.dto.request.CreateReviewRequest;
import com.clothing.dto.response.ReviewResponse;
import com.clothing.entity.AttributeEntity;
import com.clothing.entity.AttributeValueEntity;
import com.clothing.entity.OrderEntity;
import com.clothing.entity.OrderItemEntity;
import com.clothing.entity.ProductVariantEntity;
import com.clothing.entity.ReviewEntity;
import com.clothing.entity.UserEntity;
import com.clothing.entity.VariantAttributeValueEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.AttributeRepository;
import com.clothing.repository.AttributeValueRepository;
import com.clothing.repository.OrderItemRepository;
import com.clothing.repository.OrderRepository;
import com.clothing.repository.ProductVariantRepository;
import com.clothing.repository.ReviewRepository;
import com.clothing.repository.UserRepository;
import com.clothing.repository.VariantAttributeValueRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final VariantAttributeValueRepository variantAttributeValueRepository;
    private final AttributeValueRepository attributeValueRepository;
    private final AttributeRepository attributeRepository;

    public ReviewController(
            ReviewRepository reviewRepository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductVariantRepository productVariantRepository,
            VariantAttributeValueRepository variantAttributeValueRepository,
            AttributeValueRepository attributeValueRepository,
            AttributeRepository attributeRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.variantAttributeValueRepository = variantAttributeValueRepository;
        this.attributeValueRepository = attributeValueRepository;
        this.attributeRepository = attributeRepository;
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<List<ReviewResponse>> getByProduct(@PathVariable Long productId) {
        List<ReviewResponse> responses = reviewRepository.findByProductIdOrderByIdDesc(productId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ReviewResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        UserEntity user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
        OrderEntity order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new BusinessException("Order not found", HttpStatus.NOT_FOUND));
        if (!user.getId().equals(order.getUserId())) {
            throw new BusinessException("Order does not belong to current user", HttpStatus.FORBIDDEN);
        }
        if (!"DELIVERED".equalsIgnoreCase(String.valueOf(order.getStatus()))) {
            throw new BusinessException("Chỉ đánh giá được đơn đã giao", HttpStatus.BAD_REQUEST);
        }

        List<OrderItemEntity> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
        ProductVariantEntity purchasedVariant = resolvePurchasedVariant(items, request.getProductId());
        if (purchasedVariant == null) {
            throw new BusinessException("Sản phẩm không thuộc đơn hàng này", HttpStatus.BAD_REQUEST);
        }
        if (reviewRepository.existsByUserIdAndOrderIdAndProductId(user.getId(), order.getId(), request.getProductId())) {
            throw new BusinessException("Bạn đã đánh giá sản phẩm này trong đơn hàng", HttpStatus.BAD_REQUEST);
        }

        Map<String, String> attributes = resolveVariantAttributes(purchasedVariant.getId());

        ReviewEntity entity = new ReviewEntity();
        entity.setUserId(user.getId());
        entity.setOrderId(order.getId());
        entity.setProductId(request.getProductId());
        entity.setRating(request.getRating());
        entity.setComment(request.getComment() == null ? "" : request.getComment().trim());
        entity.setSize(attributes.get("size"));
        entity.setColor(attributes.get("color"));
        entity.setImageUrls(joinImageUrls(request.getImageUrls()));
        entity.setCreatedAt(LocalDateTime.now());
        ReviewEntity saved = reviewRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    private ReviewResponse toResponse(ReviewEntity entity) {
        UserEntity user = userRepository.findById(entity.getUserId()).orElse(null);
        return ReviewResponse.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .productId(entity.getProductId())
                .userId(entity.getUserId())
                .username(user == null ? ("User #" + entity.getUserId()) : user.getUsername())
                .rating(entity.getRating())
                .comment(entity.getComment())
                .size(entity.getSize())
                .color(entity.getColor())
                .imageUrls(splitImageUrls(entity.getImageUrls()))
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private ProductVariantEntity resolvePurchasedVariant(List<OrderItemEntity> items, Long productId) {
        for (OrderItemEntity item : items) {
            ProductVariantEntity variant = productVariantRepository.findById(item.getVariantId()).orElse(null);
            if (variant != null && productId.equals(variant.getProductId())) {
                return variant;
            }
        }
        return null;
    }

    private Map<String, String> resolveVariantAttributes(Long variantId) {
        String size = null;
        String color = null;
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
                size = value.getValue();
            } else if ("color".equals(attrName)) {
                color = value.getValue();
            }
        }
        return Map.of(
                "size", size == null ? "" : size,
                "color", color == null ? "" : color
        );
    }

    private String joinImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return "";
        }
        List<String> normalized = imageUrls.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(5)
                .toList();
        return String.join("\n", normalized);
    }

    private List<String> splitImageUrls(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String[] lines = raw.split("\\r?\\n");
        List<String> urls = new ArrayList<>();
        for (String line : lines) {
            if (line == null) continue;
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            urls.add(trimmed);
        }
        return urls;
    }
}
