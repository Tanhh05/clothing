package com.clothing.service.impl;

import com.clothing.dto.request.CreateOrderRequest;
import com.clothing.dto.request.UpdateOrderStatusRequest;
import com.clothing.dto.response.AdminDashboardSummaryResponse;
import com.clothing.dto.response.OrderItemResponse;
import com.clothing.dto.response.OrderResponse;
import com.clothing.dto.response.OrderStatusHistoryResponse;
import com.clothing.dto.response.PageResponse;
import com.clothing.dto.response.ProductSalesStatResponse;
import com.clothing.entity.CartEntity;
import com.clothing.entity.CartItemEntity;
import com.clothing.entity.CouponEntity;
import com.clothing.entity.CouponUsageEntity;
import com.clothing.entity.OrderEntity;
import com.clothing.entity.OrderItemEntity;
import com.clothing.entity.OrderStatusHistoryEntity;
import com.clothing.entity.ProductEntity;
import com.clothing.entity.ProductVariantEntity;
import com.clothing.entity.UserAddressEntity;
import com.clothing.entity.UserEntity;
import com.clothing.exception.BusinessException;
import com.clothing.messaging.publisher.OrderEventPublisher;
import com.clothing.repository.CartItemRepository;
import com.clothing.repository.CartRepository;
import com.clothing.repository.CouponRepository;
import com.clothing.repository.CouponUsageRepository;
import com.clothing.repository.OrderItemRepository;
import com.clothing.repository.OrderRepository;
import com.clothing.repository.OrderStatusHistoryRepository;
import com.clothing.repository.ProductRepository;
import com.clothing.repository.ProductVariantRepository;
import com.clothing.repository.UserAddressRepository;
import com.clothing.repository.UserRepository;
import com.clothing.service.GhnShippingService;
import com.clothing.service.OrderService;
import com.clothing.service.PaymentService;
import com.clothing.service.AuditLogService;
import com.clothing.service.StoreSettingService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.LocalTime;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class OrderServiceImpl implements OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private static final String STATUS_PENDING = "PENDING";
    private static final String SHIPPING_PROVIDER_GHN = "GHN";
    private static final String PAYMENT_MOMO = "MOMO";
    private static final long FAR_DISTANCE_SURCHARGE = 20_000L;
    private static final Set<String> NEAR_PROVINCES = Set.of(
            "ho chi minh",
            "binh duong",
            "dong nai",
            "long an",
            "tay ninh",
            "ba ria vung tau"
    );

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final ProductRepository productRepository;
    private final UserAddressRepository userAddressRepository;
    private final PaymentService paymentService;
    private final AuditLogService auditLogService;
    private final StoreSettingService storeSettingService;
    private final GhnShippingService ghnShippingService;

    public OrderServiceImpl(
            UserRepository userRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            CouponRepository couponRepository,
            CouponUsageRepository couponUsageRepository,
            ProductVariantRepository productVariantRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            OrderEventPublisher orderEventPublisher,
            ProductRepository productRepository,
            UserAddressRepository userAddressRepository,
            PaymentService paymentService,
            AuditLogService auditLogService,
            StoreSettingService storeSettingService,
            GhnShippingService ghnShippingService
    ) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
        this.productVariantRepository = productVariantRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.productRepository = productRepository;
        this.userAddressRepository = userAddressRepository;
        this.paymentService = paymentService;
        this.auditLogService = auditLogService;
        this.storeSettingService = storeSettingService;
        this.ghnShippingService = ghnShippingService;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(String username, CreateOrderRequest request) {
        UserEntity user = getUser(username);
        CartEntity cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("Cart not found", HttpStatus.BAD_REQUEST));
        List<CartItemEntity> cartItems = cartItemRepository.findByCartIdOrderByIdAsc(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BusinessException("Cart is empty", HttpStatus.BAD_REQUEST);
        }

        long subTotal = 0L;
        for (CartItemEntity item : cartItems) {
            ProductVariantEntity variant = getVariant(item.getVariantId());
            if (item.getQuantity() > variant.getStock()) {
                throw new BusinessException("Not enough stock for variant: " + variant.getSku(), HttpStatus.BAD_REQUEST);
            }
            subTotal += variant.getPrice() * item.getQuantity();
        }

        String province = extractProvince(request);
        long shippingFee = calculateShippingFee(subTotal, province);
        ResolvedCoupon resolvedCoupon = resolveBestCoupon(user.getId(), subTotal, request.getVoucherCode());
        long discountAmount = resolvedCoupon == null ? 0L : resolvedCoupon.discountAmount;
        long total = Math.max(0L, subTotal + shippingFee - discountAmount);

        OrderEntity order = new OrderEntity();
        order.setUserId(user.getId());
        order.setSubTotal(subTotal);
        order.setShippingFee(shippingFee);
        order.setDiscountAmount(discountAmount);
        order.setCouponId(resolvedCoupon == null ? null : resolvedCoupon.coupon.getId());
        order.setCouponCode(resolvedCoupon == null ? null : resolvedCoupon.coupon.getCode());
        order.setTotalPrice(total);
        order.setStatus(STATUS_PENDING);
        order.setPaymentMethod(request.getPaymentMethod().trim());
        order.setShippingProvider(SHIPPING_PROVIDER_GHN);
        order.setAddress(request.getAddress().trim());
        order.setCreatedAt(LocalDateTime.now());
        OrderEntity savedOrder = orderRepository.save(order);

        for (CartItemEntity item : cartItems) {
            ProductVariantEntity variant = getVariant(item.getVariantId());
            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setVariantId(variant.getId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(variant.getPrice());
            orderItemRepository.save(orderItem);
        }

        tryCreateGhnShippingOrder(savedOrder, user, request, cartItems);

        addHistory(savedOrder.getId(), STATUS_PENDING);
        if (resolvedCoupon != null) {
            CouponEntity coupon = resolvedCoupon.coupon;
            coupon.setUsedCount((coupon.getUsedCount() == null ? 0 : coupon.getUsedCount()) + 1);
            couponRepository.save(coupon);

            CouponUsageEntity usage = new CouponUsageEntity();
            usage.setCouponId(coupon.getId());
            usage.setUserId(user.getId());
            usage.setOrderId(savedOrder.getId());
            usage.setUsedAt(LocalDateTime.now());
            couponUsageRepository.save(usage);
        }

        cartItemRepository.deleteByCartId(cart.getId());
        publishOrderCreatedAfterCommit(savedOrder.getId());
        auditLogService.log("ORDER_CREATED", "ORDER", savedOrder.getId(), "Created order by user " + username);
        OrderResponse response = getMyOrderById(username, savedOrder.getId());
        if (PAYMENT_MOMO.equalsIgnoreCase(savedOrder.getPaymentMethod())) {
            String payUrl = paymentService.createMomoPayment(savedOrder);
            response.setPaymentUrl(payUrl);
        }
        return response;
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByIdDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PageResponse<OrderResponse> getAdminOrders(
            int page,
            int size,
            String sortBy,
            String direction,
            String q,
            String status,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        if (page < 0) {
            throw new BusinessException("page must be >= 0", HttpStatus.BAD_REQUEST);
        }
        if (size <= 0 || size > 100) {
            throw new BusinessException("size must be between 1 and 100", HttpStatus.BAD_REQUEST);
        }

        String safeSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, safeSortBy));

        String keyword = (q == null || q.isBlank()) ? null : q.trim().toLowerCase(Locale.ROOT);
        String normalizedStatus = (status == null || status.isBlank()) ? null : normalizeStatus(status);
        LocalDateTime from = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? null : toDate.atTime(LocalTime.MAX);

        Specification<OrderEntity> specification = Specification.where(null);

        if (normalizedStatus != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(cb.upper(cb.coalesce(root.get("status"), "")), normalizedStatus));
        }
        if (from != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        if (keyword != null) {
            specification = specification.and((root, query, cb) -> {
                Predicate byId = cb.like(root.get("id").as(String.class), "%" + keyword + "%");
                Predicate byAddress = cb.like(cb.lower(cb.coalesce(root.get("address"), "")), "%" + keyword + "%");
                return cb.or(byId, byAddress);
            });
        }

        Page<OrderEntity> orderPage = orderRepository.findAll(specification, pageable);
        List<OrderResponse> responses = orderPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<OrderResponse>builder()
                .content(responses)
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .first(orderPage.isFirst())
                .last(orderPage.isLast())
                .build();
    }

    @Override
    public AdminDashboardSummaryResponse getAdminDashboardSummary() {
        List<OrderEntity> orders = orderRepository.findAllByOrderByIdDesc();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        long revenueToday = 0L;
        long revenue7d = 0L;
        long revenue30d = 0L;
        long ordersToday = 0L;
        long orders7d = 0L;
        long orders30d = 0L;
        long pendingOrders = 0L;
        long cancelLike30d = 0L;
        long total30d = 0L;
        Map<String, Long> statusCounts30d = new HashMap<>();
        Set<Long> orderIds30dDelivered = new HashSet<>();

        for (OrderEntity order : orders) {
            LocalDateTime createdAt = order.getCreatedAt();
            if (createdAt == null) {
                continue;
            }
            LocalDate orderDate = createdAt.toLocalDate();
            long diffDays = today.toEpochDay() - orderDate.toEpochDay();
            if (diffDays < 0) {
                continue;
            }

            String status = normalizeStatus(order.getStatus());
            boolean delivered = "DELIVERED".equals(status);
            long totalPrice = order.getTotalPrice() == null ? 0L : order.getTotalPrice();

            if (diffDays == 0) {
                ordersToday += 1;
                if (delivered) {
                    revenueToday += totalPrice;
                }
            }
            if (diffDays <= 6) {
                orders7d += 1;
                if (delivered) {
                    revenue7d += totalPrice;
                }
            }
            if (diffDays <= 29) {
                orders30d += 1;
                total30d += 1;
                statusCounts30d.merge(status, 1L, Long::sum);
                if (delivered) {
                    revenue30d += totalPrice;
                    orderIds30dDelivered.add(order.getId());
                }
                if (isCancelLikeStatus(status)) {
                    cancelLike30d += 1;
                }
            }

            if ("PENDING".equals(status) || "PROCESSING".equals(status) || "CONFIRMED".equals(status)) {
                pendingOrders += 1;
            }
        }

        double cancelRate = total30d == 0 ? 0D : (cancelLike30d * 100.0D) / total30d;
        List<ProductSalesStatResponse> topProducts30d = buildTopProducts(orderIds30dDelivered);

        return AdminDashboardSummaryResponse.builder()
                .revenueToday(revenueToday)
                .revenue7d(revenue7d)
                .revenue30d(revenue30d)
                .ordersToday(ordersToday)
                .orders7d(orders7d)
                .orders30d(orders30d)
                .pendingOrders(pendingOrders)
                .cancelRate30d(Math.round(cancelRate * 10.0D) / 10.0D)
                .statusCounts30d(statusCounts30d)
                .topProducts30d(topProducts30d)
                .build();
    }

    @Override
    public List<OrderResponse> getMyOrders(String username) {
        UserEntity user = getUser(username);
        return orderRepository.findByUserIdOrderByIdDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public OrderResponse getMyOrderById(String username, Long orderId) {
        UserEntity user = getUser(username);
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order not found", HttpStatus.NOT_FOUND));
        if (!order.getUserId().equals(user.getId())) {
            throw new BusinessException("Order does not belong to current user", HttpStatus.FORBIDDEN);
        }
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse reorder(String username, Long orderId) {
        UserEntity user = getUser(username);
        OrderEntity oldOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order not found", HttpStatus.NOT_FOUND));
        if (!user.getId().equals(oldOrder.getUserId())) {
            throw new BusinessException("Order does not belong to current user", HttpStatus.FORBIDDEN);
        }

        List<OrderItemEntity> oldItems = orderItemRepository.findByOrderIdOrderByIdAsc(orderId);
        if (oldItems.isEmpty()) {
            throw new BusinessException("Order has no items", HttpStatus.BAD_REQUEST);
        }

        CartEntity cart = cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            CartEntity created = new CartEntity();
            created.setUserId(user.getId());
            created.setCreatedAt(LocalDateTime.now());
            return cartRepository.save(created);
        });

        for (OrderItemEntity oldItem : oldItems) {
            ProductVariantEntity variant = getVariant(oldItem.getVariantId());
            int available = Math.max(0, variant.getStock() == null ? 0 : variant.getStock());
            if (available <= 0) continue;
            int quantity = Math.max(1, Math.min(oldItem.getQuantity() == null ? 1 : oldItem.getQuantity(), available));
            CartItemEntity existing = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId()).orElse(null);
            if (existing == null) {
                CartItemEntity item = new CartItemEntity();
                item.setCartId(cart.getId());
                item.setVariantId(variant.getId());
                item.setQuantity(quantity);
                cartItemRepository.save(item);
            } else {
                int next = Math.min(existing.getQuantity() + quantity, available);
                existing.setQuantity(next);
                cartItemRepository.save(existing);
            }
        }
        return toResponse(oldOrder);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order not found", HttpStatus.NOT_FOUND));
        String requestedStatus = normalizeStatus(request.getStatus());
        if (requestedStatus.isBlank()) {
            throw new BusinessException("status is required", HttpStatus.BAD_REQUEST);
        }

        String requestShippingCode = normalizeShippingCode(request.getShippingCode());
        if (!requestShippingCode.isBlank()) {
            order.setShippingProvider(SHIPPING_PROVIDER_GHN);
            order.setShippingCode(requestShippingCode);
            orderRepository.save(order);
        }

        String shippingCode = normalizeShippingCode(order.getShippingCode());
        boolean shouldSyncWithGhn = !shippingCode.isBlank() && !Boolean.FALSE.equals(request.getSyncWithGhn());
        if (shouldSyncWithGhn) {
            String syncedStatus = syncStatusFromGhn(order, false);
            if (!requestedStatus.equals(syncedStatus)) {
                throw new BusinessException(
                        "GHN status mismatch. Requested " + requestedStatus + " but GHN is " + syncedStatus,
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        String prevStatus = normalizeStatus(order.getStatus());
        if (requestedStatus.equals(prevStatus)) {
            return toResponse(order);
        }
        validateStatusTransition(prevStatus, requestedStatus, orderId);
        applyOrderStatus(order, requestedStatus, false);
        auditLogService.log(
                "ORDER_STATUS_UPDATED",
                "ORDER",
                orderId,
                "Status " + prevStatus + " -> " + requestedStatus
        );
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse syncOrderStatusWithGhn(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order not found", HttpStatus.NOT_FOUND));
        syncStatusFromGhn(order, true);
        return toResponse(order);
    }

    @Override
    @Transactional
    public void handleGhnWebhook(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        String orderCode = normalizeShippingCode(toText(payload.get("order_code")));
        String ghnStatus = ghnShippingService.normalizeGhnStatus(toText(payload.get("status")));
        if (orderCode.isBlank() || ghnStatus.isBlank()) {
            return;
        }

        OrderEntity order = orderRepository.findByShippingCode(orderCode).orElse(null);
        if (order == null) {
            return;
        }

        order.setShippingProvider(SHIPPING_PROVIDER_GHN);
        order.setShippingStatus(ghnStatus);
        order.setShippingUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        String nextOrderStatus = normalizeStatus(ghnShippingService.toInternalOrderStatus(ghnStatus));
        if (nextOrderStatus.isBlank()) {
            return;
        }
        if (nextOrderStatus.equals(normalizeStatus(order.getStatus()))) {
            return;
        }
        if (!isAllowedExternalTransition(normalizeStatus(order.getStatus()), nextOrderStatus)) {
            return;
        }

        applyOrderStatus(order, nextOrderStatus, true);
        auditLogService.log(
                "ORDER_STATUS_SYNCED_GHN_WEBHOOK",
                "ORDER",
                order.getId(),
                "Webhook synced by GHN: " + nextOrderStatus + " (" + ghnStatus + ")"
        );
    }

    @Override
    @Transactional
    public int bulkUpdateStatus(List<Long> ids, String status) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("ids must not be empty", HttpStatus.BAD_REQUEST);
        }
        String normalizedStatus = normalizeStatus(status);
        if (normalizedStatus.isBlank()) {
            throw new BusinessException("status is required", HttpStatus.BAD_REQUEST);
        }

        int affected = 0;
        for (Long id : ids) {
            OrderEntity order = orderRepository.findById(id).orElse(null);
            if (order == null) {
                continue;
            }
            String prevStatus = normalizeStatus(order.getStatus());
            if (normalizedStatus.equals(prevStatus)) {
                continue;
            }
            validateStatusTransition(prevStatus, normalizedStatus, id);
            order.setStatus(normalizedStatus);
            orderRepository.save(order);
            addHistory(order.getId(), normalizedStatus);
            affected += 1;
        }

        auditLogService.log(
                "ORDER_BULK_STATUS_UPDATED",
                "ORDER",
                null,
                "Set status " + normalizedStatus + " for " + affected + " orders"
        );
        return affected;
    }

    private List<ProductSalesStatResponse> buildTopProducts(Set<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> quantityByProductId = new HashMap<>();
        Map<Long, Long> revenueByProductId = new HashMap<>();
        List<OrderItemEntity> allItems = orderItemRepository.findAll();

        for (OrderItemEntity item : allItems) {
            if (!orderIds.contains(item.getOrderId())) {
                continue;
            }
            ProductVariantEntity variant = productVariantRepository.findById(item.getVariantId()).orElse(null);
            if (variant == null) {
                continue;
            }
            Long productId = variant.getProductId();
            long quantity = item.getQuantity() == null ? 0L : item.getQuantity();
            long lineRevenue = (item.getPrice() == null ? 0L : item.getPrice()) * quantity;
            quantityByProductId.merge(productId, quantity, Long::sum);
            revenueByProductId.merge(productId, lineRevenue, Long::sum);
        }

        return quantityByProductId.entrySet()
                .stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(entry -> {
                    Long productId = entry.getKey();
                    ProductEntity product = productRepository.findById(productId).orElse(null);
                    return ProductSalesStatResponse.builder()
                            .productId(productId)
                            .productName(product == null ? ("Product #" + productId) : product.getName())
                            .totalQuantity(entry.getValue())
                            .totalRevenue(revenueByProductId.getOrDefault(productId, 0L))
                            .build();
                })
                .toList();
    }

    private boolean isCancelLikeStatus(String status) {
        return "CANCELLED".equals(status)
                || "FAILED".equals(status)
                || "FAILED_DELIVERY".equals(status)
                || "FAILED_INSUFFICIENT_STOCK".equals(status)
                || "REFUNDED".equals(status);
    }

    private String normalizeStatus(String status) {
        return String.valueOf(status == null ? "" : status).trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeShippingCode(String shippingCode) {
        if (shippingCode == null) {
            return "";
        }
        return shippingCode.trim();
    }

    private String toText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private void validateStatusTransition(String currentStatus, String targetStatus, Long orderId) {
        if (currentStatus.equals(targetStatus)) {
            return;
        }
        if (!isAllowedTransition(currentStatus, targetStatus)) {
            throw new BusinessException(
                    "Cannot change order #" + orderId + " status from " + currentStatus + " to " + targetStatus,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private boolean isAllowedTransition(String currentStatus, String targetStatus) {
        return switch (currentStatus) {
            case "PENDING" -> Set.of("PROCESSING", "CANCELLED", "FAILED").contains(targetStatus);
            case "PROCESSING" -> Set.of("CONFIRMED", "CANCELLED", "FAILED").contains(targetStatus);
            case "CONFIRMED" -> Set.of("SHIPPED", "CANCELLED", "FAILED").contains(targetStatus);
            case "SHIPPED" -> Set.of("DELIVERED", "FAILED", "FAILED_DELIVERY").contains(targetStatus);
            case "CANCELLED", "FAILED", "FAILED_DELIVERY", "RETURN_REQUESTED" -> "REFUNDED".equals(targetStatus);
            default -> false;
        };
    }

    private boolean isAllowedExternalTransition(String currentStatus, String targetStatus) {
        if (currentStatus.equals(targetStatus)) {
            return true;
        }
        if (isAllowedTransition(currentStatus, targetStatus)) {
            return true;
        }
        return switch (currentStatus) {
            case "PENDING" -> Set.of("CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED", "FAILED_DELIVERY").contains(targetStatus);
            case "PROCESSING" -> Set.of("SHIPPED", "DELIVERED", "CANCELLED", "FAILED_DELIVERY").contains(targetStatus);
            case "CONFIRMED" -> Set.of("DELIVERED").contains(targetStatus);
            case "SHIPPED" -> Set.of("CANCELLED").contains(targetStatus);
            default -> false;
        };
    }

    private String syncStatusFromGhn(OrderEntity order, boolean forceApply) {
        String shippingCode = normalizeShippingCode(order.getShippingCode());
        if (shippingCode.isBlank()) {
            throw new BusinessException("Order has no shippingCode", HttpStatus.BAD_REQUEST);
        }

        GhnShippingService.GhnOrderDetail detail = ghnShippingService.getOrderDetail(shippingCode);
        String ghnStatus = ghnShippingService.normalizeGhnStatus(detail.status());
        String mappedStatus = normalizeStatus(ghnShippingService.toInternalOrderStatus(ghnStatus));
        if (mappedStatus.isBlank()) {
            throw new BusinessException("GHN status is not mapped yet: " + ghnStatus, HttpStatus.BAD_REQUEST);
        }

        order.setShippingProvider(SHIPPING_PROVIDER_GHN);
        order.setShippingStatus(ghnStatus);
        order.setShippingUpdatedAt(LocalDateTime.now());
        if (detail.orderCode() != null && !detail.orderCode().isBlank()) {
            order.setShippingCode(detail.orderCode().trim());
        }
        orderRepository.save(order);

        String currentStatus = normalizeStatus(order.getStatus());
        if (mappedStatus.equals(currentStatus)) {
            return mappedStatus;
        }
        if (!isAllowedExternalTransition(currentStatus, mappedStatus)) {
            throw new BusinessException(
                    "Cannot sync order #" + order.getId() + " from " + currentStatus + " to " + mappedStatus,
                    HttpStatus.BAD_REQUEST
            );
        }
        if (forceApply) {
            applyOrderStatus(order, mappedStatus, true);
            auditLogService.log(
                    "ORDER_STATUS_SYNCED_GHN",
                    "ORDER",
                    order.getId(),
                    "Synced status by GHN detail: " + mappedStatus + " (" + ghnStatus + ")"
            );
        }
        return mappedStatus;
    }

    private void applyOrderStatus(OrderEntity order, String status, boolean externalSync) {
        String normalized = normalizeStatus(status);
        String current = normalizeStatus(order.getStatus());
        if (normalized.equals(current)) {
            return;
        }
        order.setStatus(normalized);
        if (externalSync) {
            order.setShippingUpdatedAt(LocalDateTime.now());
        }
        orderRepository.save(order);
        addHistory(order.getId(), normalized);
    }

    private OrderResponse toResponse(OrderEntity order) {
        UserEntity customer = userRepository.findById(order.getUserId()).orElse(null);
        String customerName = customer == null
                ? null
                : (customer.getFullName() != null && !customer.getFullName().isBlank()
                    ? customer.getFullName().trim()
                    : customer.getUsername());

        List<OrderItemEntity> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
        List<OrderItemResponse> itemResponses = items.stream().map(item -> {
            ProductVariantEntity variant = getVariant(item.getVariantId());
            ProductEntity product = productRepository.findById(variant.getProductId()).orElse(null);
            return OrderItemResponse.builder()
                    .id(item.getId())
                    .productId(product == null ? null : product.getId())
                    .variantId(item.getVariantId())
                    .sku(variant.getSku())
                    .productName(product == null ? null : product.getName())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .lineTotal(item.getPrice() * item.getQuantity())
                    .build();
        }).toList();

        List<OrderStatusHistoryResponse> historyResponses = orderStatusHistoryRepository.findByOrderIdOrderByIdAsc(order.getId())
                .stream()
                .map(h -> OrderStatusHistoryResponse.builder()
                        .status(h.getStatus())
                        .changedAt(h.getChangedAt())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .customerName(customerName)
                .totalPrice(order.getTotalPrice())
                .subTotal(order.getSubTotal())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .appliedVoucherCode(order.getCouponCode())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentUrl(null)
                .shippingProvider(order.getShippingProvider())
                .shippingCode(order.getShippingCode())
                .shippingStatus(order.getShippingStatus())
                .shippingUpdatedAt(order.getShippingUpdatedAt())
                .address(order.getAddress())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .statusHistory(historyResponses)
                .build();
    }

    private void addHistory(Long orderId, String status) {
        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setOrderId(orderId);
        history.setStatus(status);
        history.setChangedAt(LocalDateTime.now());
        orderStatusHistoryRepository.save(history);
    }

    private UserEntity getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
    }

    private ProductVariantEntity getVariant(Long variantId) {
        return productVariantRepository.findById(variantId)
                .orElseThrow(() -> new BusinessException("Variant not found", HttpStatus.BAD_REQUEST));
    }

    private void publishOrderCreatedAfterCommit(Long orderId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    orderEventPublisher.publishOrderCreated(orderId);
                }
            });
            return;
        }
        orderEventPublisher.publishOrderCreated(orderId);
    }

    private void tryCreateGhnShippingOrder(
            OrderEntity order,
            UserEntity user,
            CreateOrderRequest request,
            List<CartItemEntity> cartItems
    ) {
        order.setShippingProvider(SHIPPING_PROVIDER_GHN);

        if (!ghnShippingService.canCallShippingApi()) {
            throw new BusinessException("GHN shipping config is incomplete", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ReceiverInfo receiver = resolveReceiverInfo(user, request);
        if (receiver.address.isBlank() || receiver.district.isBlank() || receiver.ward.isBlank() || receiver.phone.isBlank()) {
            throw new BusinessException(
                    "Thiếu thông tin nhận hàng để tạo đơn GHN (address/district/ward/phone)",
                    HttpStatus.BAD_REQUEST
            );
        }
        if (!isValidShippingPhone(receiver.phone)) {
            log.warn("Reject create GHN for order {}: invalid phone {}", order.getId(), receiver.phone);
            throw new BusinessException(
                    "Số điện thoại nhận hàng không hợp lệ. Vui lòng nhập đúng 10 số (VD: 09xxxxxxxx)",
                    HttpStatus.BAD_REQUEST
            );
        }

        long totalWeightGrams = estimateTotalWeightGrams(cartItems);
        long codAmount = "COD".equalsIgnoreCase(order.getPaymentMethod())
                ? (order.getTotalPrice() == null ? 0L : order.getTotalPrice())
                : 0L;

        String clientOrderCode = "ORD-" + order.getId();
        String shippingCode = ghnShippingService.createShippingOrder(new GhnShippingService.CreateShippingOrderRequest(
                clientOrderCode,
                receiver.recipientName,
                receiver.phone,
                receiver.address,
                receiver.province,
                receiver.district,
                receiver.ward,
                codAmount,
                totalWeightGrams
        ));

        if (shippingCode == null || shippingCode.isBlank()) {
            throw new BusinessException("GHN không trả về mã vận đơn", HttpStatus.BAD_GATEWAY);
        }

        order.setShippingCode(shippingCode);
        order.setShippingStatus("ready_to_pick");
        order.setShippingUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    private long estimateTotalWeightGrams(List<CartItemEntity> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return 500L;
        }
        long total = 0L;
        for (CartItemEntity item : cartItems) {
            ProductVariantEntity variant = getVariant(item.getVariantId());
            double itemWeightKg = variant.getWeight() == null || variant.getWeight() <= 0
                    ? 0.2D
                    : variant.getWeight();
            long itemWeightGrams = Math.round(itemWeightKg * 1000D);
            int quantity = item.getQuantity() == null || item.getQuantity() <= 0 ? 1 : item.getQuantity();
            total += Math.max(50L, itemWeightGrams) * quantity;
        }
        return Math.max(50L, total);
    }

    private ReceiverInfo resolveReceiverInfo(UserEntity user, CreateOrderRequest request) {
        UserAddressEntity defaultAddress = userAddressRepository.findByUserIdAndIsDefaultTrue(user.getId()).orElse(null);

        String recipientName = firstNonBlank(
                request.getRecipientName(),
                defaultAddress == null ? null : defaultAddress.getRecipientName(),
                user.getFullName(),
                user.getUsername()
        );
        String phone = firstNonBlank(
                request.getPhone(),
                defaultAddress == null ? null : defaultAddress.getPhone(),
                user.getPhone()
        );
        String province = firstNonBlank(
                request.getProvince(),
                defaultAddress == null ? null : defaultAddress.getProvince()
        );
        String district = firstNonBlank(
                request.getDistrict(),
                defaultAddress == null ? null : defaultAddress.getDistrict()
        );
        String ward = firstNonBlank(
                request.getWard(),
                defaultAddress == null ? null : defaultAddress.getWard()
        );
        String address = firstNonBlank(
                request.getAddress(),
                defaultAddress == null ? null : defaultAddress.getAddressLine()
        );

        return new ReceiverInfo(
                safeTrim(recipientName),
                normalizePhoneForShipping(safeTrim(phone)),
                safeTrim(province),
                safeTrim(district),
                safeTrim(ward),
                safeTrim(address)
        );
    }

    private String normalizePhoneForShipping(String phone) {
        String normalized = safeTrim(phone).replaceAll("\\s+", "");
        if (normalized.startsWith("+84")) {
            return "0" + normalized.substring(3);
        }
        if (normalized.startsWith("84")) {
            return "0" + normalized.substring(2);
        }
        return normalized;
    }

    private boolean isValidShippingPhone(String phone) {
        return phone != null && phone.matches("^0\\d{9}$");
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String extractProvince(CreateOrderRequest request) {
        if (request.getProvince() != null && !request.getProvince().isBlank()) {
            return request.getProvince().trim();
        }
        String address = request.getAddress();
        if (address == null || address.isBlank()) {
            return "";
        }
        String[] parts = address.split(",");
        if (parts.length == 0) {
            return address.trim();
        }
        return parts[parts.length - 1].trim();
    }

    private long calculateShippingFee(long subTotal, String province) {
        var settings = storeSettingService.getSettings();
        long defaultFee = settings.getDefaultShippingFee() == null ? 30_000L : settings.getDefaultShippingFee();
        long freeShippingThreshold = settings.getFreeShippingThreshold() == null ? 500_000L : settings.getFreeShippingThreshold();

        if (freeShippingThreshold > 0 && subTotal >= freeShippingThreshold) {
            return 0L;
        }

        String normalizedProvince = normalizeProvince(province);
        if (NEAR_PROVINCES.contains(normalizedProvince)) {
            return defaultFee;
        }
        return defaultFee + FAR_DISTANCE_SURCHARGE;
    }

    private ResolvedCoupon resolveBestCoupon(Long userId, long subTotal, String preferredCode) {
        LocalDateTime now = LocalDateTime.now();
        List<CouponEntity> candidates = couponRepository.findAll().stream()
                .filter(coupon -> "ACTIVE".equalsIgnoreCase(String.valueOf(coupon.getStatus())))
                .filter(coupon -> coupon.getStartDate() == null || !coupon.getStartDate().isAfter(now))
                .filter(coupon -> coupon.getEndDate() == null || !coupon.getEndDate().isBefore(now))
                .filter(coupon -> coupon.getMinOrderValue() == null || subTotal >= coupon.getMinOrderValue())
                .filter(coupon -> coupon.getQuantity() == null || (coupon.getUsedCount() == null ? 0 : coupon.getUsedCount()) < coupon.getQuantity())
                .filter(coupon -> !couponUsageRepository.existsByCouponIdAndUserId(coupon.getId(), userId))
                .toList();

        if (candidates.isEmpty()) return null;

        CouponEntity preferred = null;
        if (preferredCode != null && !preferredCode.isBlank()) {
            String normalized = preferredCode.trim().toUpperCase(Locale.ROOT);
            preferred = candidates.stream()
                    .filter(coupon -> normalized.equalsIgnoreCase(coupon.getCode()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Voucher không hợp lệ hoặc không áp dụng được", HttpStatus.BAD_REQUEST));
        }

        if (preferred != null) {
            long discount = calculateCouponDiscount(preferred, subTotal);
            if (discount <= 0) {
                throw new BusinessException("Voucher không tạo được giảm giá", HttpStatus.BAD_REQUEST);
            }
            return new ResolvedCoupon(preferred, discount);
        }

        return candidates.stream()
                .map(coupon -> new ResolvedCoupon(coupon, calculateCouponDiscount(coupon, subTotal)))
                .filter(result -> result.discountAmount > 0)
                .max(Comparator.comparingLong(value -> value.discountAmount))
                .orElse(null);
    }

    private long calculateCouponDiscount(CouponEntity coupon, long subTotal) {
        if (coupon == null) return 0L;
        String discountType = String.valueOf(coupon.getDiscountType()).trim().toUpperCase(Locale.ROOT);
        long discountValue = coupon.getDiscountValue() == null ? 0L : coupon.getDiscountValue();
        if (discountValue <= 0 || subTotal <= 0) return 0L;

        long discount = 0L;
        if ("PERCENT".equals(discountType)) {
            discount = Math.round(subTotal * (discountValue / 100.0d));
        } else if ("AMOUNT".equals(discountType)) {
            discount = discountValue;
        }
        long maxDiscount = coupon.getMaxDiscountValue() == null ? Long.MAX_VALUE : Math.max(0L, coupon.getMaxDiscountValue());
        discount = Math.min(discount, maxDiscount);
        return Math.max(0L, Math.min(discount, subTotal));
    }

    private String normalizeProvince(String province) {
        if (province == null || province.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(province, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.startsWith("thanh pho ")) {
            normalized = normalized.substring("thanh pho ".length()).trim();
        } else if (normalized.startsWith("tp ")) {
            normalized = normalized.substring("tp ".length()).trim();
        } else if (normalized.startsWith("tinh ")) {
            normalized = normalized.substring("tinh ".length()).trim();
        }

        String[] tokens = normalized.split(" ");
        List<String> filtered = Arrays.stream(tokens)
                .filter(token -> !token.isBlank())
                .toList();
        return String.join(" ", filtered);
    }

    private static class ResolvedCoupon {
        private final CouponEntity coupon;
        private final long discountAmount;

        private ResolvedCoupon(CouponEntity coupon, long discountAmount) {
            this.coupon = coupon;
            this.discountAmount = discountAmount;
        }
    }

    private static class ReceiverInfo {
        private final String recipientName;
        private final String phone;
        private final String province;
        private final String district;
        private final String ward;
        private final String address;

        private ReceiverInfo(String recipientName, String phone, String province, String district, String ward, String address) {
            this.recipientName = recipientName;
            this.phone = phone;
            this.province = province;
            this.district = district;
            this.ward = ward;
            this.address = address;
        }
    }
}
