package com.clothing.service.impl;

import com.clothing.dto.request.CreateReturnRequest;
import com.clothing.dto.request.CreateReturnRequestItem;
import com.clothing.dto.response.ReturnRequestItemResponse;
import com.clothing.dto.response.ReturnRequestResponse;
import com.clothing.entity.OrderEntity;
import com.clothing.entity.OrderItemEntity;
import com.clothing.entity.OrderStatusHistoryEntity;
import com.clothing.entity.ProductVariantEntity;
import com.clothing.entity.ReturnRequestEntity;
import com.clothing.entity.ReturnRequestItemEntity;
import com.clothing.entity.UserEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.OrderItemRepository;
import com.clothing.repository.OrderRepository;
import com.clothing.repository.OrderStatusHistoryRepository;
import com.clothing.repository.ProductRepository;
import com.clothing.repository.ProductVariantRepository;
import com.clothing.repository.ReturnRequestItemRepository;
import com.clothing.repository.ReturnRequestRepository;
import com.clothing.repository.UserRepository;
import com.clothing.service.AuditLogService;
import com.clothing.service.ReturnRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReturnRequestServiceImpl implements ReturnRequestService {

    private static final int RETURN_WINDOW_DAYS = 7;
    private static final Set<String> ALLOWED_RETURN_TYPES = Set.of("REFUND", "EXCHANGE");
    private static final Set<String> ACTIVE_STATUSES = Set.of(
            "REQUESTED",
            "UNDER_REVIEW",
            "RETURN_APPROVED",
            "IN_TRANSIT_BACK",
            "RETURN_RECEIVED",
            "REFUND_PROCESSING",
            "EXCHANGE_PROCESSING"
    );
    private static final Set<String> ALLOWED_REASON_CODES = Set.of(
            "NOT_AS_DESCRIBED",
            "DEFECTIVE",
            "WRONG_ITEM",
            "WRONG_SIZE",
            "CHANGE_MIND",
            "MISSING_PARTS",
            "OTHER"
    );

    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnRequestItemRepository returnRequestItemRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final AuditLogService auditLogService;

    public ReturnRequestServiceImpl(
            ReturnRequestRepository returnRequestRepository,
            ReturnRequestItemRepository returnRequestItemRepository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductVariantRepository productVariantRepository,
            ProductRepository productRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            AuditLogService auditLogService
    ) {
        this.returnRequestRepository = returnRequestRepository;
        this.returnRequestItemRepository = returnRequestItemRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.productRepository = productRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<ReturnRequestResponse> getAll(String status) {
        List<ReturnRequestEntity> entities;
        if (status == null || status.isBlank()) {
            entities = returnRequestRepository.findAllByOrderByIdDesc();
        } else {
            entities = returnRequestRepository.findByStatusOrderByIdDesc(normalize(status));
        }
        return entities.stream().map(this::toResponse).toList();
    }

    @Override
    public List<ReturnRequestResponse> getMyRequests(String username, String status) {
        UserEntity user = getUser(username);
        List<ReturnRequestEntity> entities;
        if (status == null || status.isBlank()) {
            entities = returnRequestRepository.findByUserIdOrderByIdDesc(user.getId());
        } else {
            entities = returnRequestRepository.findByUserIdAndStatusOrderByIdDesc(user.getId(), normalize(status));
        }
        return entities.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ReturnRequestResponse create(String username, CreateReturnRequest request) {
        UserEntity user = getUser(username);
        OrderEntity order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new BusinessException("Order not found", HttpStatus.NOT_FOUND));

        if (order.getUserId() == null || !order.getUserId().equals(user.getId())) {
            throw new BusinessException("You cannot create return request for this order", HttpStatus.FORBIDDEN);
        }

        String orderStatus = normalize(order.getStatus());
        if (!"DELIVERED".equals(orderStatus) && !"RETURN_REQUESTED".equals(orderStatus)) {
            throw new BusinessException("Only delivered orders can be returned", HttpStatus.BAD_REQUEST);
        }

        LocalDateTime deliveredAt = orderStatusHistoryRepository
                .findTopByOrderIdAndStatusOrderByChangedAtDesc(order.getId(), "DELIVERED")
                .map(OrderStatusHistoryEntity::getChangedAt)
                .orElse(order.getCreatedAt());
        if (deliveredAt == null) {
            deliveredAt = LocalDateTime.now();
        }
        if (deliveredAt.plusDays(RETURN_WINDOW_DAYS).isBefore(LocalDateTime.now())) {
            throw new BusinessException("Return window is closed (7 days after delivery)", HttpStatus.BAD_REQUEST);
        }

        if (returnRequestRepository.existsByOrderIdAndStatusIn(order.getId(), ACTIVE_STATUSES)) {
            throw new BusinessException("This order already has an active return request", HttpStatus.CONFLICT);
        }

        String returnType = normalize(request.getReturnType());
        if (!ALLOWED_RETURN_TYPES.contains(returnType)) {
            throw new BusinessException("Unsupported return type", HttpStatus.BAD_REQUEST);
        }

        String reasonCode = normalize(request.getReasonCode());
        if (!ALLOWED_REASON_CODES.contains(reasonCode)) {
            throw new BusinessException("Unsupported reason code", HttpStatus.BAD_REQUEST);
        }

        Map<Long, OrderItemEntity> orderItemMap = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId())
                .stream()
                .collect(Collectors.toMap(OrderItemEntity::getId, item -> item));
        if (orderItemMap.isEmpty()) {
            throw new BusinessException("Order has no items", HttpStatus.BAD_REQUEST);
        }

        ReturnRequestEntity entity = new ReturnRequestEntity();
        entity.setOrderId(order.getId());
        entity.setUserId(user.getId());
        entity.setCustomerName(resolveCustomerName(user));
        entity.setReturnType(returnType);
        entity.setReasonCode(reasonCode);
        entity.setReasonDetail(request.getReasonDetail().trim());
        entity.setEvidenceUrls(sanitizeNullable(request.getEvidenceUrls()));
        entity.setReason(reasonCode + " - " + request.getReasonDetail().trim());
        entity.setStatus("REQUESTED");
        entity.setRequestedAt(LocalDateTime.now());
        entity.setDeliveredAt(deliveredAt);
        entity.setResolvedAt(null);
        entity.setResolutionNote(null);
        ReturnRequestEntity saved = returnRequestRepository.save(entity);

        saveReturnItems(saved.getId(), request.getItems(), orderItemMap);
        moveOrderToReturnRequested(order);

        auditLogService.log(
                "RETURN_REQUEST_CREATED",
                "RETURN_REQUEST",
                saved.getId(),
                "Created return request for order #" + saved.getOrderId()
        );
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ReturnRequestResponse updateStatus(Long id, String status, String note) {
        ReturnRequestEntity entity = returnRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Return request not found", HttpStatus.NOT_FOUND));
        String next = normalize(status);
        validateTransition(entity.getStatus(), next, entity.getReturnType());

        entity.setStatus(next);
        if ("RETURN_REJECTED".equals(next) || "REFUNDED".equals(next) || "EXCHANGED".equals(next)) {
            entity.setResolvedAt(LocalDateTime.now());
        }
        if (note != null && !note.isBlank()) {
            entity.setResolutionNote(note.trim());
        }

        ReturnRequestEntity saved = returnRequestRepository.save(entity);
        syncOrderStatusOnReturnProgress(saved);

        auditLogService.log("RETURN_STATUS_UPDATED", "RETURN_REQUEST", saved.getId(), "Updated status to " + next);
        return toResponse(saved);
    }

    private void saveReturnItems(
            Long returnRequestId,
            List<CreateReturnRequestItem> items,
            Map<Long, OrderItemEntity> orderItemMap
    ) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("At least one return item is required", HttpStatus.BAD_REQUEST);
        }
        Map<Long, Integer> mergedQty = new HashMap<>();
        for (CreateReturnRequestItem item : items) {
            Long orderItemId = item.getOrderItemId();
            Integer quantity = item.getQuantity();
            if (orderItemId == null || quantity == null || quantity <= 0) {
                throw new BusinessException("Invalid return item payload", HttpStatus.BAD_REQUEST);
            }
            mergedQty.merge(orderItemId, quantity, Integer::sum);
        }

        for (Map.Entry<Long, Integer> entry : mergedQty.entrySet()) {
            Long orderItemId = entry.getKey();
            int requestedQty = entry.getValue();
            OrderItemEntity orderItem = orderItemMap.get(orderItemId);
            if (orderItem == null) {
                throw new BusinessException("Return item does not belong to this order", HttpStatus.BAD_REQUEST);
            }
            if (orderItem.getQuantity() == null || requestedQty > orderItem.getQuantity()) {
                throw new BusinessException("Requested quantity exceeds purchased quantity", HttpStatus.BAD_REQUEST);
            }

            ProductVariantEntity variant = productVariantRepository.findById(orderItem.getVariantId())
                    .orElseThrow(() -> new BusinessException("Variant not found for return item", HttpStatus.BAD_REQUEST));
            String productName = productRepository.findById(variant.getProductId())
                    .map(product -> product.getName() == null ? "" : product.getName())
                    .orElse("");

            ReturnRequestItemEntity row = new ReturnRequestItemEntity();
            row.setReturnRequestId(returnRequestId);
            row.setOrderItemId(orderItemId);
            row.setVariantId(orderItem.getVariantId());
            row.setSku(variant.getSku());
            row.setProductName(productName);
            row.setRequestedQuantity(requestedQty);
            returnRequestItemRepository.save(row);
        }
    }

    private void moveOrderToReturnRequested(OrderEntity order) {
        String current = normalize(order.getStatus());
        if ("RETURN_REQUESTED".equals(current)) {
            return;
        }
        order.setStatus("RETURN_REQUESTED");
        orderRepository.save(order);
        addOrderHistory(order.getId(), "RETURN_REQUESTED");
    }

    private void syncOrderStatusOnReturnProgress(ReturnRequestEntity request) {
        OrderEntity order = orderRepository.findById(request.getOrderId()).orElse(null);
        if (order == null) {
            return;
        }
        String returnStatus = normalize(request.getStatus());
        if ("REFUNDED".equals(returnStatus)) {
            if (!"REFUNDED".equals(normalize(order.getStatus()))) {
                order.setStatus("REFUNDED");
                orderRepository.save(order);
                addOrderHistory(order.getId(), "REFUNDED");
            }
            return;
        }
        if ("EXCHANGED".equals(returnStatus) || "RETURN_REJECTED".equals(returnStatus)) {
            if (!"DELIVERED".equals(normalize(order.getStatus()))) {
                order.setStatus("DELIVERED");
                orderRepository.save(order);
                addOrderHistory(order.getId(), "DELIVERED");
            }
        }
    }

    private void addOrderHistory(Long orderId, String status) {
        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setOrderId(orderId);
        history.setStatus(status);
        history.setChangedAt(LocalDateTime.now());
        orderStatusHistoryRepository.save(history);
    }

    private void validateTransition(String currentStatus, String nextStatus, String returnType) {
        String current = normalize(currentStatus);
        String type = normalize(returnType);

        if ("REQUESTED".equals(current)) {
            if (Set.of("UNDER_REVIEW", "RETURN_REJECTED").contains(nextStatus)) {
                return;
            }
            throwInvalidTransition(current, nextStatus);
        }
        if ("UNDER_REVIEW".equals(current)) {
            if (Set.of("RETURN_APPROVED", "RETURN_REJECTED").contains(nextStatus)) {
                return;
            }
            throwInvalidTransition(current, nextStatus);
        }
        if ("RETURN_APPROVED".equals(current)) {
            if ("IN_TRANSIT_BACK".equals(nextStatus)) {
                return;
            }
            throwInvalidTransition(current, nextStatus);
        }
        if ("IN_TRANSIT_BACK".equals(current)) {
            if ("RETURN_RECEIVED".equals(nextStatus)) {
                return;
            }
            throwInvalidTransition(current, nextStatus);
        }
        if ("RETURN_RECEIVED".equals(current)) {
            if ("REFUND".equals(type) && "REFUND_PROCESSING".equals(nextStatus)) {
                return;
            }
            if ("EXCHANGE".equals(type) && "EXCHANGE_PROCESSING".equals(nextStatus)) {
                return;
            }
            throwInvalidTransition(current, nextStatus);
        }
        if ("REFUND_PROCESSING".equals(current) && "REFUNDED".equals(nextStatus)) {
            return;
        }
        if ("EXCHANGE_PROCESSING".equals(current) && "EXCHANGED".equals(nextStatus)) {
            return;
        }
        throw new BusinessException("Cannot transition from " + current, HttpStatus.BAD_REQUEST);
    }

    private void throwInvalidTransition(String current, String next) {
        throw new BusinessException("Invalid transition from " + current + " to " + next, HttpStatus.BAD_REQUEST);
    }

    private ReturnRequestResponse toResponse(ReturnRequestEntity entity) {
        List<ReturnRequestItemResponse> items = returnRequestItemRepository
                .findByReturnRequestIdOrderByIdAsc(entity.getId())
                .stream()
                .map(item -> ReturnRequestItemResponse.builder()
                        .id(item.getId())
                        .orderItemId(item.getOrderItemId())
                        .variantId(item.getVariantId())
                        .sku(item.getSku())
                        .productName(item.getProductName())
                        .requestedQuantity(item.getRequestedQuantity())
                        .build())
                .toList();

        return ReturnRequestResponse.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .userId(entity.getUserId())
                .customer(entity.getCustomerName())
                .returnType(entity.getReturnType())
                .reasonCode(entity.getReasonCode())
                .reasonDetail(entity.getReasonDetail())
                .evidenceUrls(entity.getEvidenceUrls())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .requestedAt(entity.getRequestedAt())
                .deliveredAt(entity.getDeliveredAt())
                .resolvedAt(entity.getResolvedAt())
                .resolutionNote(entity.getResolutionNote())
                .items(items)
                .build();
    }

    private UserEntity getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
    }

    private String resolveCustomerName(UserEntity user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName().trim();
        }
        return user.getUsername();
    }

    private String sanitizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
