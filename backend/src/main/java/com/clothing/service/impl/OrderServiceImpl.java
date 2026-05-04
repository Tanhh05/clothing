package com.clothing.service.impl;

import com.clothing.dto.request.CreateOrderRequest;
import com.clothing.dto.request.PosCheckoutItemRequest;
import com.clothing.dto.request.PosCheckoutRequest;
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
import com.clothing.entity.MomoCheckoutSessionEntity;
import com.clothing.entity.OrderEntity;
import com.clothing.entity.OrderItemEntity;
import com.clothing.entity.OrderStatusHistoryEntity;
import com.clothing.entity.PaymentEntity;
import com.clothing.entity.ProductEntity;
import com.clothing.entity.ProductVariantEntity;
import com.clothing.entity.StockReservationEntity;
import com.clothing.entity.UserAddressEntity;
import com.clothing.entity.UserEntity;
import com.clothing.entity.VnpayCheckoutSessionEntity;
import com.clothing.exception.BusinessException;
import com.clothing.messaging.publisher.OrderEventPublisher;
import com.clothing.repository.CartItemRepository;
import com.clothing.repository.CartRepository;
import com.clothing.repository.CouponRepository;
import com.clothing.repository.CouponUsageRepository;
import com.clothing.repository.DashboardMetricsProjection;
import com.clothing.repository.MomoCheckoutSessionRepository;
import com.clothing.repository.OrderItemRepository;
import com.clothing.repository.OrderRepository;
import com.clothing.repository.OrderStatusHistoryRepository;
import com.clothing.repository.PaymentRepository;
import com.clothing.repository.ProductRepository;
import com.clothing.repository.ProductVariantRepository;
import com.clothing.repository.StockReservationRepository;
import com.clothing.repository.StatusCountProjection;
import com.clothing.repository.TopProductSalesProjection;
import com.clothing.repository.UserAddressRepository;
import com.clothing.repository.UserRepository;
import com.clothing.repository.VnpayCheckoutSessionRepository;
import com.clothing.service.GhnShippingService;
import com.clothing.service.InventoryMovementService;
import com.clothing.service.OrderService;
import com.clothing.service.PaymentService;
import com.clothing.service.EmailService;
import com.clothing.service.AuditLogService;
import com.clothing.service.StoreSettingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.text.Normalizer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_WAITING_PAYMENT = "WAITING_PAYMENT";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String SHIPPING_PROVIDER_GHN = "GHN";
    private static final String SHIPPING_PROVIDER_POS_COUNTER = "POS_COUNTER";
    private static final String SHIPPING_PROVIDER_POS_SHIP = "POS_SHIP";
    private static final String STATUS_DELIVERED = "DELIVERED";
    private static final String INVENTORY_TYPE_POS = "POS_DEDUCT";
    private static final String INVENTORY_TYPE_POS_REVERT = "POS_REVERT";
    private static final String INVENTORY_TYPE_POS_CAPTURE = "POS_CAPTURE";
    private static final String INVENTORY_TYPE_CLIENT_CAPTURE = "CLIENT_CAPTURE";
    private static final String INVENTORY_TYPE_COD_CAPTURE = "COD_CAPTURE";
    private static final String PAYMENT_COD = "COD";
    private static final String PAYMENT_MOMO = "MOMO";
    private static final String PAYMENT_VNPAY = "VNPAY";
    private static final String PAYMENT_STATUS_PAID = "PAID";
    private static final String PAYMENT_STATUS_FAILED = "FAILED";
    private static final String MOMO_SESSION_STATUS_PENDING = "PENDING";
    private static final String MOMO_SESSION_STATUS_COMPLETED = "COMPLETED";
    private static final String MOMO_SESSION_STATUS_EXPIRED = "EXPIRED";
    private static final String VNPAY_SESSION_STATUS_PENDING = "PENDING";
    private static final String VNPAY_SESSION_STATUS_COMPLETED = "COMPLETED";
    private static final String VNPAY_SESSION_STATUS_EXPIRED = "EXPIRED";
    private static final long MOMO_SESSION_TTL_MINUTES = 30L;
    private static final long VNPAY_SESSION_TTL_MINUTES = 30L;
    private static final long COD_RESERVATION_TTL_MINUTES = 120L;
    private static final String RESERVATION_STATUS_ACTIVE = "ACTIVE";
    private static final String RESERVATION_STATUS_CONVERTED = "CONVERTED";
    private static final String RESERVATION_STATUS_RELEASED = "RELEASED";
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
    private final MomoCheckoutSessionRepository momoCheckoutSessionRepository;
    private final VnpayCheckoutSessionRepository vnpayCheckoutSessionRepository;
    private final ProductVariantRepository productVariantRepository;
    private final StockReservationRepository stockReservationRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final ProductRepository productRepository;
    private final UserAddressRepository userAddressRepository;
    private final PaymentService paymentService;
    private final AuditLogService auditLogService;
    private final StoreSettingService storeSettingService;
    private final GhnShippingService ghnShippingService;
    private final InventoryMovementService inventoryMovementService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    public OrderServiceImpl(
            UserRepository userRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            CouponRepository couponRepository,
            CouponUsageRepository couponUsageRepository,
            MomoCheckoutSessionRepository momoCheckoutSessionRepository,
            VnpayCheckoutSessionRepository vnpayCheckoutSessionRepository,
            ProductVariantRepository productVariantRepository,
            StockReservationRepository stockReservationRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            PaymentRepository paymentRepository,
            OrderEventPublisher orderEventPublisher,
            ProductRepository productRepository,
            UserAddressRepository userAddressRepository,
            PaymentService paymentService,
            AuditLogService auditLogService,
            StoreSettingService storeSettingService,
            GhnShippingService ghnShippingService,
            InventoryMovementService inventoryMovementService,
            EmailService emailService,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
        this.momoCheckoutSessionRepository = momoCheckoutSessionRepository;
        this.vnpayCheckoutSessionRepository = vnpayCheckoutSessionRepository;
        this.productVariantRepository = productVariantRepository;
        this.stockReservationRepository = stockReservationRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.paymentRepository = paymentRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.productRepository = productRepository;
        this.userAddressRepository = userAddressRepository;
        this.paymentService = paymentService;
        this.auditLogService = auditLogService;
        this.storeSettingService = storeSettingService;
        this.ghnShippingService = ghnShippingService;
        this.inventoryMovementService = inventoryMovementService;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
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

        String paymentMethod = safeTrim(request.getPaymentMethod()).toUpperCase(Locale.ROOT);
        if (paymentMethod.isBlank()) {
            throw new BusinessException("paymentMethod is required", HttpStatus.BAD_REQUEST);
        }

        long subTotal = 0L;
        for (CartItemEntity item : cartItems) {
            ProductVariantEntity variant = getVariant(item.getVariantId());
            int stock = variant.getStock() == null ? 0 : variant.getStock();
            int availableForNewOrder = stock - getActiveReservedQuantity(variant.getId());
            if (item.getQuantity() > availableForNewOrder) {
                throw new BusinessException("Not enough stock for variant: " + variant.getSku(), HttpStatus.BAD_REQUEST);
            }
            subTotal += variant.getPrice() * item.getQuantity();
        }

        String province = extractProvince(request);
        long shippingFee = resolveShippingFee(request, subTotal, province);
        ResolvedCoupon resolvedCoupon = resolveBestCoupon(user.getId(), subTotal, request.getVoucherCode());
        long discountAmount = resolvedCoupon == null ? 0L : resolvedCoupon.discountAmount;
        long total = Math.max(0L, subTotal + shippingFee - discountAmount);

        if (PAYMENT_MOMO.equalsIgnoreCase(paymentMethod)) {
            return createMomoCheckoutSession(user, request, cartItems, subTotal, shippingFee, discountAmount, total, resolvedCoupon);
        }
        if (PAYMENT_VNPAY.equalsIgnoreCase(paymentMethod)) {
            return createVnpayCheckoutSession(user, request, cartItems, subTotal, shippingFee, discountAmount, total, resolvedCoupon);
        }
        boolean reserveCodStock = PAYMENT_COD.equalsIgnoreCase(paymentMethod);
        Map<Long, ProductVariantEntity> lockedVariants = new HashMap<>();
        if (reserveCodStock) {
            Map<Long, Integer> requestedQtyByVariantId = new HashMap<>();
            for (CartItemEntity item : cartItems) {
                if (item.getVariantId() == null) {
                    throw new BusinessException("variantId is required", HttpStatus.BAD_REQUEST);
                }
                int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
                if (quantity <= 0) {
                    throw new BusinessException("quantity must be >= 1", HttpStatus.BAD_REQUEST);
                }
                requestedQtyByVariantId.merge(item.getVariantId(), quantity, Integer::sum);
            }
            for (Long variantId : requestedQtyByVariantId.keySet().stream().sorted().toList()) {
                ProductVariantEntity lockedVariant = productVariantRepository.findByIdForUpdate(variantId)
                        .orElseThrow(() -> new BusinessException("Variant not found", HttpStatus.BAD_REQUEST));
                int stock = lockedVariant.getStock() == null ? 0 : lockedVariant.getStock();
                int requested = requestedQtyByVariantId.getOrDefault(variantId, 0);
                int availableForNewOrder = stock - getActiveReservedQuantity(variantId);
                if (availableForNewOrder < requested) {
                    throw new BusinessException("Not enough stock for variant: " + lockedVariant.getSku(), HttpStatus.BAD_REQUEST);
                }
                lockedVariants.put(variantId, lockedVariant);
            }
        }

        OrderEntity order = new OrderEntity();
        order.setUserId(user.getId());
        order.setSubTotal(subTotal);
        order.setShippingFee(shippingFee);
        order.setDiscountAmount(discountAmount);
        order.setCouponId(resolvedCoupon == null ? null : resolvedCoupon.coupon.getId());
        order.setCouponCode(resolvedCoupon == null ? null : resolvedCoupon.coupon.getCode());
        order.setTotalPrice(total);
        order.setStatus(STATUS_PENDING);
        order.setPaymentMethod(paymentMethod);
        order.setShippingProvider(SHIPPING_PROVIDER_GHN);
        order.setAddress(request.getAddress().trim());
        order.setCreatedAt(LocalDateTime.now());
        OrderEntity savedOrder = orderRepository.save(order);

        for (CartItemEntity item : cartItems) {
            ProductVariantEntity variant = reserveCodStock
                    ? lockedVariants.get(item.getVariantId())
                    : getVariant(item.getVariantId());
            if (variant == null) {
                throw new BusinessException("Variant not found", HttpStatus.BAD_REQUEST);
            }
            if (reserveCodStock) {
                reserveStockForCodOrder(savedOrder, variant.getId(), item.getQuantity());
            }
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
        return getMyOrderById(username, savedOrder.getId());
    }

    @Override
    @Transactional
    public OrderResponse createPosOrder(String adminUsername, PosCheckoutRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("items is required", HttpStatus.BAD_REQUEST);
        }

        UserEntity admin = getUser(adminUsername);
        UserEntity customer = request.getCustomerId() == null ? null : userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new BusinessException("Customer not found", HttpStatus.BAD_REQUEST));
        Long orderUserId = customer == null ? admin.getId() : customer.getId();
        Long couponEligibilityUserId = customer == null ? null : customer.getId();

        List<PosCheckoutItemRequest> items = request.getItems();
        Map<Long, Integer> requestedQtyByVariantId = new HashMap<>();
        for (PosCheckoutItemRequest item : items) {
            if (item.getVariantId() == null) {
                throw new BusinessException("variantId is required", HttpStatus.BAD_REQUEST);
            }
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if (quantity <= 0) {
                throw new BusinessException("quantity must be >= 1", HttpStatus.BAD_REQUEST);
            }
            requestedQtyByVariantId.merge(item.getVariantId(), quantity, Integer::sum);
        }

        Map<Long, ProductVariantEntity> lockedVariants = new HashMap<>();
        for (Long variantId : requestedQtyByVariantId.keySet().stream().sorted().toList()) {
            ProductVariantEntity lockedVariant = productVariantRepository.findByIdForUpdate(variantId)
                    .orElseThrow(() -> new BusinessException("Variant not found", HttpStatus.BAD_REQUEST));
            int stock = lockedVariant.getStock() == null ? 0 : lockedVariant.getStock();
            int requested = requestedQtyByVariantId.getOrDefault(variantId, 0);
            int availableForNewOrder = stock - getActiveReservedQuantity(variantId);
            if (availableForNewOrder < requested) {
                throw new BusinessException("Not enough stock for variant: " + lockedVariant.getSku(), HttpStatus.BAD_REQUEST);
            }
            lockedVariants.put(variantId, lockedVariant);
        }

        long subTotal = 0L;
        for (PosCheckoutItemRequest item : items) {
            ProductVariantEntity variant = lockedVariants.get(item.getVariantId());
            if (variant == null) {
                throw new BusinessException("Variant not found", HttpStatus.BAD_REQUEST);
            }
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            subTotal += (variant.getPrice() == null ? 0L : variant.getPrice()) * quantity;
        }

        long shippingFee = Boolean.TRUE.equals(request.getShipEnabled())
                ? Math.max(0L, request.getShippingFee() == null ? 0L : request.getShippingFee())
                : 0L;
        long manualDiscount = Math.max(0L, request.getManualDiscount() == null ? 0L : request.getManualDiscount());
        manualDiscount = Math.min(manualDiscount, subTotal);

        ResolvedCoupon resolvedCoupon = resolveBestCoupon(couponEligibilityUserId, subTotal, request.getVoucherCode());
        long couponDiscount = resolvedCoupon == null ? 0L : resolvedCoupon.discountAmount;
        long discountAmount = Math.min(subTotal, couponDiscount + manualDiscount);
        long total = Math.max(0L, subTotal + shippingFee - discountAmount);

        String paymentMethod = safeTrim(request.getPaymentMethod());
        if (paymentMethod.isBlank()) {
            throw new BusinessException("paymentMethod is required", HttpStatus.BAD_REQUEST);
        }
        String normalizedPaymentMethod = paymentMethod.toUpperCase(Locale.ROOT);
        boolean useMomoPosPayment = "BANK_TRANSFER".equals(normalizedPaymentMethod) || PAYMENT_MOMO.equals(normalizedPaymentMethod);

        // POS counter allows checkout without forcing "paid amount" input.
        // Cash handover reconciliation can be handled outside checkout flow.

        if (Boolean.TRUE.equals(request.getShipEnabled())) {
            validatePosShippingFields(request);
        }

        OrderEntity order = new OrderEntity();
        order.setUserId(orderUserId);
        order.setSubTotal(subTotal);
        order.setShippingFee(shippingFee);
        order.setDiscountAmount(discountAmount);
        order.setCouponId(resolvedCoupon == null ? null : resolvedCoupon.coupon.getId());
        order.setCouponCode(resolvedCoupon == null ? null : resolvedCoupon.coupon.getCode());
        order.setTotalPrice(total);
        order.setPaymentMethod(useMomoPosPayment ? PAYMENT_MOMO : normalizedPaymentMethod);
        order.setStatus(useMomoPosPayment
                ? STATUS_WAITING_PAYMENT
                : (Boolean.TRUE.equals(request.getShipEnabled()) ? "CONFIRMED" : STATUS_DELIVERED));
        order.setShippingProvider(Boolean.TRUE.equals(request.getShipEnabled()) ? SHIPPING_PROVIDER_POS_SHIP : SHIPPING_PROVIDER_POS_COUNTER);
        order.setAddress(buildPosAddress(request, customer));
        order.setNote(safeTrim(request.getNote()));
        order.setCreatedAt(LocalDateTime.now());
        OrderEntity savedOrder = orderRepository.save(order);

        for (PosCheckoutItemRequest item : items) {
            ProductVariantEntity variant = lockedVariants.get(item.getVariantId());
            if (variant == null) {
                throw new BusinessException("Variant not found", HttpStatus.BAD_REQUEST);
            }
            if (useMomoPosPayment) {
                reserveStockForMomoPosOrder(savedOrder, variant.getId(), item.getQuantity());
            } else {
                inventoryMovementService.deductStockByVariantId(
                        variant.getId(),
                        item.getQuantity(),
                        INVENTORY_TYPE_POS,
                        "POS checkout order #" + savedOrder.getId()
                );
            }
            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setVariantId(variant.getId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(variant.getPrice());
            orderItemRepository.save(orderItem);
        }

        addHistory(savedOrder.getId(), savedOrder.getStatus());
        if (resolvedCoupon != null) {
            CouponEntity coupon = resolvedCoupon.coupon;
            coupon.setUsedCount((coupon.getUsedCount() == null ? 0 : coupon.getUsedCount()) + 1);
            couponRepository.save(coupon);

            if (couponEligibilityUserId != null) {
                CouponUsageEntity usage = new CouponUsageEntity();
                usage.setCouponId(coupon.getId());
                usage.setUserId(couponEligibilityUserId);
                usage.setOrderId(savedOrder.getId());
                usage.setUsedAt(LocalDateTime.now());
                couponUsageRepository.save(usage);
            }
        }

        auditLogService.log(
                "POS_ORDER_CREATED",
                "ORDER",
                savedOrder.getId(),
                "POS checkout by admin " + adminUsername + ", payment " + paymentMethod + ", total " + total
        );

        if (useMomoPosPayment) {
            String payUrl = paymentService.createMomoPayment(savedOrder, "captureWallet");
            return toResponseWithPaymentUrl(savedOrder, payUrl);
        }

        return toResponse(savedOrder);
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        List<OrderEntity> orders = orderRepository.findAllByOrderByIdDesc();
        return toResponses(orders);
    }

    @Override
    public PageResponse<OrderResponse> getAdminOrders(
            int page,
            int size,
            String sortBy,
            String direction,
            String q,
            String status,
            String shippingStatus,
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
        String normalizedShippingStatus = (shippingStatus == null || shippingStatus.isBlank())
                ? null
                : ghnShippingService.normalizeGhnStatus(shippingStatus);
        LocalDateTime from = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? null : toDate.atTime(LocalTime.MAX);

        Specification<OrderEntity> specification = Specification.where(null);

        if (normalizedStatus != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(cb.upper(cb.coalesce(root.get("status"), "")), normalizedStatus));
        }
        if (normalizedShippingStatus != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(cb.lower(cb.coalesce(root.get("shippingStatus"), "")), normalizedShippingStatus));
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
        List<OrderResponse> responses = toResponses(orderPage.getContent());

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
        DashboardMetricsProjection metrics = orderRepository.fetchDashboardMetrics();
        long revenueToday = metrics == null ? 0L : safeLong(metrics.getRevenueToday());
        long revenue7d = metrics == null ? 0L : safeLong(metrics.getRevenue7d());
        long revenue30d = metrics == null ? 0L : safeLong(metrics.getRevenue30d());
        long ordersToday = metrics == null ? 0L : safeLong(metrics.getOrdersToday());
        long orders7d = metrics == null ? 0L : safeLong(metrics.getOrders7d());
        long orders30d = metrics == null ? 0L : safeLong(metrics.getOrders30d());
        long pendingOrders = metrics == null ? 0L : safeLong(metrics.getPendingOrders());
        long cancelLike30d = metrics == null ? 0L : safeLong(metrics.getCancelLike30d());
        long total30d = metrics == null ? 0L : safeLong(metrics.getTotal30d());

        Map<String, Long> statusCounts30d = new HashMap<>();
        for (StatusCountProjection row : orderRepository.findStatusCounts30d()) {
            statusCounts30d.put(normalizeStatus(row.getStatus()), safeLong(row.getTotal()));
        }

        double cancelRate = total30d == 0 ? 0D : (cancelLike30d * 100.0D) / total30d;
        List<ProductSalesStatResponse> topProducts30d = buildTopProducts();

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
    public Map<String, List<String>> getAdminStatusOptions() {
        List<String> orderStatuses = orderRepository.findDistinctOrderStatuses().stream()
                .map(this::normalizeStatus)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList();

        List<String> ghnShippingStatuses = orderRepository.findDistinctShippingStatuses().stream()
                .map(ghnShippingService::normalizeGhnStatus)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList();

        Map<String, List<String>> options = new HashMap<>();
        options.put("orderStatuses", orderStatuses);
        options.put("ghnShippingStatuses", ghnShippingStatuses);
        return options;
    }

    @Override
    public List<OrderResponse> getMyOrders(String username) {
        UserEntity user = getUser(username);
        List<OrderEntity> orders = orderRepository.findByUserIdOrderByIdDesc(user.getId());
        return toResponses(orders);
    }

    @Override
    public PageResponse<OrderResponse> getMyOrders(String username, int page, int size) {
        if (page < 0) {
            throw new BusinessException("page must be >= 0", HttpStatus.BAD_REQUEST);
        }
        if (size < 1 || size > 100) {
            throw new BusinessException("size must be between 1 and 100", HttpStatus.BAD_REQUEST);
        }
        UserEntity user = getUser(username);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<OrderEntity> orderPage = orderRepository.findByUserId(user.getId(), pageable);
        List<OrderResponse> responses = toResponses(orderPage.getContent());
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
    public OrderResponse getMyOrderById(String username, Long orderId) {
        UserEntity user = getUser(username);
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order not found", HttpStatus.NOT_FOUND));
        if (!order.getUserId().equals(user.getId())) {
            throw new BusinessException("Order does not belong to current user", HttpStatus.FORBIDDEN);
        }
        return toResponses(List.of(order)).get(0);
    }

    @Override
    public void sendMyOrderConfirmationEmail(String username, Long orderId) {
        UserEntity user = getUser(username);
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order not found", HttpStatus.NOT_FOUND));
        if (!order.getUserId().equals(user.getId())) {
            throw new BusinessException("Order does not belong to current user", HttpStatus.FORBIDDEN);
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BusinessException("Email not found", HttpStatus.BAD_REQUEST);
        }
        emailService.sendOrderConfirmationEmail(user.getEmail().trim(), order.getId());
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
    public void handleMomoPaymentIpn(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        int resultCode = parseInt(payload.get("resultCode"), -1);
        if (resultCode != 0) {
            return;
        }

        String sessionToken = extractMomoSessionToken(payload.get("extraData"));
        if (sessionToken.isBlank()) {
            syncPosOrderStatusFromMomoPayment(payload);
            return;
        }

        MomoCheckoutSessionEntity session = momoCheckoutSessionRepository.findByTokenForUpdate(sessionToken).orElse(null);
        if (session == null) {
            return;
        }
        if (session.getCreatedOrderId() != null) {
            OrderEntity order = orderRepository.findById(session.getCreatedOrderId()).orElse(null);
            if (order != null && STATUS_WAITING_PAYMENT.equalsIgnoreCase(safeTrim(order.getStatus()))) {
                if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now())) {
                    applyOrderStatus(order, STATUS_CANCELLED, false);
                    session.setStatus(MOMO_SESSION_STATUS_EXPIRED);
                    session.setConsumedAt(LocalDateTime.now());
                    momoCheckoutSessionRepository.save(session);
                    return;
                }

                List<MomoCartItemSnapshot> snapshots = parseCartSnapshot(session.getCartItemsPayload());
                if (snapshots.isEmpty()) {
                    throw new BusinessException("MoMo session cart is empty", HttpStatus.BAD_REQUEST);
                }
                CreateOrderRequest request = parseCreateOrderRequestSnapshot(session.getRequestPayload());
                UserEntity user = userRepository.findById(session.getUserId())
                        .orElseThrow(() -> new BusinessException("User not found for MoMo session", HttpStatus.NOT_FOUND));

                List<CartItemEntity> pseudoCartItems = toPseudoCartItems(snapshots);
                try {
                    tryCreateGhnShippingOrder(order, user, request, pseudoCartItems);
                } catch (Exception ex) {
                    log.error("Cannot create GHN shipping order after MoMo paid. orderId={}", order.getId(), ex);
                    order.setShippingStatus("CREATE_FAILED");
                    order.setShippingUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order);
                }

                applyOrderStatus(order, STATUS_PENDING, false);

                if (session.getCouponId() != null) {
                    CouponEntity coupon = couponRepository.findById(session.getCouponId()).orElse(null);
                    if (coupon != null) {
                        coupon.setUsedCount((coupon.getUsedCount() == null ? 0 : coupon.getUsedCount()) + 1);
                        couponRepository.save(coupon);

                        CouponUsageEntity usage = new CouponUsageEntity();
                        usage.setCouponId(coupon.getId());
                        usage.setUserId(order.getUserId());
                        usage.setOrderId(order.getId());
                        usage.setUsedAt(LocalDateTime.now());
                        couponUsageRepository.save(usage);
                    }
                }

                CartEntity cart = cartRepository.findByUserId(order.getUserId()).orElse(null);
                if (cart != null) {
                    cartItemRepository.deleteByCartId(cart.getId());
                }
                publishOrderCreatedAfterCommit(order.getId());
                auditLogService.log("ORDER_CREATED", "ORDER", order.getId(), "Created order after MoMo payment");
            }

            session.setStatus(MOMO_SESSION_STATUS_COMPLETED);
            session.setConsumedAt(LocalDateTime.now());
            session.setPaidAt(LocalDateTime.now());
            session.setPaymentTransactionCode(toText(payload.get("orderId")));
            momoCheckoutSessionRepository.save(session);
            return;
        }

        List<MomoCartItemSnapshot> snapshots = parseCartSnapshot(session.getCartItemsPayload());
        if (snapshots.isEmpty()) {
            throw new BusinessException("MoMo session cart is empty", HttpStatus.BAD_REQUEST);
        }
        CreateOrderRequest request = parseCreateOrderRequestSnapshot(session.getRequestPayload());
        UserEntity user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new BusinessException("User not found for MoMo session", HttpStatus.NOT_FOUND));

        OrderEntity createdOrder = createOrderFromMomoSession(session, user, request, snapshots);
        session.setStatus(MOMO_SESSION_STATUS_COMPLETED);
        session.setCreatedOrderId(createdOrder.getId());
        session.setPaidAt(LocalDateTime.now());
        session.setConsumedAt(LocalDateTime.now());
        session.setPaymentTransactionCode(toText(payload.get("orderId")));
        momoCheckoutSessionRepository.save(session);
    }

    private void syncPosOrderStatusFromMomoPayment(Map<String, Object> payload) {
        String transactionCode = toText(payload.get("orderId"));
        if (transactionCode.isBlank()) {
            return;
        }

        PaymentEntity payment = paymentRepository.findByTransactionCode(transactionCode).orElse(null);
        if (payment == null || payment.getOrderId() == null) {
            return;
        }
        OrderEntity order = orderRepository.findById(payment.getOrderId()).orElse(null);
        if (order == null) {
            return;
        }
        if (!PAYMENT_MOMO.equalsIgnoreCase(safeTrim(order.getPaymentMethod()))) {
            return;
        }
        if (!STATUS_WAITING_PAYMENT.equalsIgnoreCase(safeTrim(order.getStatus()))) {
            return;
        }

        String nextStatus = SHIPPING_PROVIDER_POS_SHIP.equalsIgnoreCase(safeTrim(order.getShippingProvider()))
                ? "CONFIRMED"
                : STATUS_DELIVERED;
        applyOrderStatus(order, nextStatus, false);
        auditLogService.log(
                "POS_ORDER_PAID_MOMO",
                "ORDER",
                order.getId(),
                "POS order paid by MoMo transaction " + transactionCode + ", status -> " + nextStatus
        );
    }

    @Override
    @Transactional
    public void handleVnpayPaymentIpn(Map<String, String> payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        String responseCode = safeTrim(payload.get("vnp_ResponseCode"));
        String txnStatus = safeTrim(payload.get("vnp_TransactionStatus"));
        boolean success = "00".equals(responseCode) && (txnStatus.isBlank() || "00".equals(txnStatus));
        if (!success) {
            return;
        }

        String txnRef = safeTrim(payload.get("vnp_TxnRef"));
        String sessionToken = extractVnpaySessionTokenFromTxnRef(txnRef);
        if (sessionToken.isBlank()) {
            return;
        }

        VnpayCheckoutSessionEntity session = vnpayCheckoutSessionRepository.findByTokenForUpdate(sessionToken).orElse(null);
        if (session == null) {
            return;
        }
        if (session.getExpiresAt() != null
                && session.getExpiresAt().isBefore(LocalDateTime.now())
                && VNPAY_SESSION_STATUS_PENDING.equalsIgnoreCase(session.getStatus())) {
            session.setStatus(VNPAY_SESSION_STATUS_EXPIRED);
            session.setConsumedAt(LocalDateTime.now());
            vnpayCheckoutSessionRepository.save(session);
            return;
        }
        if (session.getCreatedOrderId() != null) {
            if (!VNPAY_SESSION_STATUS_COMPLETED.equalsIgnoreCase(session.getStatus())) {
                session.setStatus(VNPAY_SESSION_STATUS_COMPLETED);
                session.setConsumedAt(LocalDateTime.now());
                vnpayCheckoutSessionRepository.save(session);
            }
            return;
        }

        List<MomoCartItemSnapshot> snapshots = parseCartSnapshot(session.getCartItemsPayload());
        if (snapshots.isEmpty()) {
            throw new BusinessException("VNPAY session cart is empty", HttpStatus.BAD_REQUEST);
        }
        CreateOrderRequest request = parseCreateOrderRequestSnapshot(session.getRequestPayload());
        UserEntity user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new BusinessException("User not found for VNPAY session", HttpStatus.NOT_FOUND));

        OrderEntity createdOrder = createOrderFromVnpaySession(session, user, request, snapshots);
        session.setStatus(VNPAY_SESSION_STATUS_COMPLETED);
        session.setCreatedOrderId(createdOrder.getId());
        session.setPaidAt(LocalDateTime.now());
        session.setConsumedAt(LocalDateTime.now());
        session.setPaymentTransactionCode(txnRef);
        vnpayCheckoutSessionRepository.save(session);
    }

    @Override
    @Transactional
    public int cancelExpiredMomoWaitingPaymentOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(MOMO_SESSION_TTL_MINUTES);
        List<OrderEntity> expiredOrders = orderRepository.findByStatusIgnoreCaseAndPaymentMethodIgnoreCaseAndCreatedAtBefore(
                STATUS_WAITING_PAYMENT,
                PAYMENT_MOMO,
                cutoff
        );
        if (expiredOrders.isEmpty()) {
            return 0;
        }

        int cancelled = 0;
        for (OrderEntity order : expiredOrders) {
            if (!STATUS_WAITING_PAYMENT.equalsIgnoreCase(safeTrim(order.getStatus()))) {
                continue;
            }
            applyOrderStatus(order, STATUS_CANCELLED, false);
            paymentRepository.findByOrderId(order.getId()).ifPresent(payment -> {
                if (PAYMENT_STATUS_PAID.equalsIgnoreCase(safeTrim(payment.getStatus()))) {
                    return;
                }
                payment.setStatus(PAYMENT_STATUS_FAILED);
                paymentRepository.save(payment);
            });
            auditLogService.log(
                    "MOMO_ORDER_TIMEOUT_CANCELLED",
                    "ORDER",
                    order.getId(),
                    "Auto-cancel MoMo waiting-payment order due to timeout"
            );
            cancelled += 1;
        }
        return cancelled;
    }

    @Override
    @Transactional
    public int cancelExpiredCodReservedOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(COD_RESERVATION_TTL_MINUTES);
        List<OrderEntity> expiredOrders = orderRepository.findByStatusIgnoreCaseAndPaymentMethodIgnoreCaseAndCreatedAtBefore(
                "CONFIRMED",
                PAYMENT_COD,
                cutoff
        );
        if (expiredOrders.isEmpty()) {
            return 0;
        }

        int cancelled = 0;
        for (OrderEntity order : expiredOrders) {
            if (!"CONFIRMED".equalsIgnoreCase(safeTrim(order.getStatus()))) {
                continue;
            }
            if (!stockReservationRepository.existsByOrderIdAndStatusIgnoreCase(order.getId(), RESERVATION_STATUS_ACTIVE)) {
                continue;
            }
            applyOrderStatus(order, STATUS_CANCELLED, false);
            auditLogService.log(
                    "COD_ORDER_TIMEOUT_CANCELLED",
                    "ORDER",
                    order.getId(),
                    "Auto-cancel COD reserved order due to timeout before shipped"
            );
            cancelled += 1;
        }
        return cancelled;
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

    @Override
    public List<OrderResponse> getAdminInvoices(List<Long> ids) {
        return fetchInvoicesByIds(ids);
    }

    @Override
    public byte[] exportAdminInvoicesExcel(List<Long> ids) {
        List<OrderResponse> invoices = fetchInvoicesByIds(ids);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Invoices");
            String[] headers = {
                    "Ma don",
                    "Khach hang",
                    "Trang thai",
                    "Thanh toan",
                    "Van chuyen",
                    "Tong tien",
                    "Tam tinh",
                    "Phi ship",
                    "Giam gia",
                    "Dia chi",
                    "Tao luc"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIndex = 1;
            for (OrderResponse invoice : invoices) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(invoice.getId() == null ? "" : String.valueOf(invoice.getId()));
                row.createCell(1).setCellValue(nonNullText(invoice.getCustomerName(), "Khach le"));
                row.createCell(2).setCellValue(nonNullText(normalizeStatus(invoice.getStatus()), ""));
                row.createCell(3).setCellValue(nonNullText(invoice.getPaymentMethod(), ""));
                row.createCell(4).setCellValue(nonNullText(invoice.getShippingStatus(), ""));
                row.createCell(5).setCellValue(safeLong(invoice.getTotalPrice()));
                row.createCell(6).setCellValue(safeLong(invoice.getSubTotal()));
                row.createCell(7).setCellValue(safeLong(invoice.getShippingFee()));
                row.createCell(8).setCellValue(safeLong(invoice.getDiscountAmount()));
                row.createCell(9).setCellValue(nonNullText(invoice.getAddress(), ""));
                row.createCell(10).setCellValue(invoice.getCreatedAt() == null ? "" : invoice.getCreatedAt().toString());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("Cannot export invoices excel", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private OrderResponse createMomoCheckoutSession(
            UserEntity user,
            CreateOrderRequest request,
            List<CartItemEntity> cartItems,
            long subTotal,
            long shippingFee,
            long discountAmount,
            long total,
            ResolvedCoupon resolvedCoupon
    ) {
        if (!ghnShippingService.canCallShippingApi()) {
            throw new BusinessException("GHN shipping config is incomplete", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        ReceiverInfo receiverInfo = resolveReceiverInfo(user, request);
        if (receiverInfo.address.isBlank()
                || receiverInfo.district.isBlank()
                || receiverInfo.ward.isBlank()
                || receiverInfo.phone.isBlank()) {
            throw new BusinessException(
                    "Thiếu thông tin nhận hàng để tạo đơn GHN (address/district/ward/phone)",
                    HttpStatus.BAD_REQUEST
            );
        }
        if (!isValidShippingPhone(receiverInfo.phone)) {
            throw new BusinessException(
                    "Số điện thoại nhận hàng không hợp lệ. Vui lòng nhập đúng 10 số (VD: 09xxxxxxxx)",
                    HttpStatus.BAD_REQUEST
            );
        }

        MomoCheckoutSessionEntity session = new MomoCheckoutSessionEntity();
        String token = UUID.randomUUID().toString().replace("-", "");
        Map<Long, Integer> requestedQtyByVariantId = new HashMap<>();
        for (CartItemEntity item : cartItems) {
            if (item.getVariantId() == null) {
                throw new BusinessException("variantId is required", HttpStatus.BAD_REQUEST);
            }
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if (quantity <= 0) {
                throw new BusinessException("quantity must be >= 1", HttpStatus.BAD_REQUEST);
            }
            requestedQtyByVariantId.merge(item.getVariantId(), quantity, Integer::sum);
        }
        Map<Long, ProductVariantEntity> lockedVariants = new HashMap<>();
        for (Long variantId : requestedQtyByVariantId.keySet().stream().sorted().toList()) {
            ProductVariantEntity lockedVariant = productVariantRepository.findByIdForUpdate(variantId)
                    .orElseThrow(() -> new BusinessException("Variant not found", HttpStatus.BAD_REQUEST));
            int stock = lockedVariant.getStock() == null ? 0 : lockedVariant.getStock();
            int requested = requestedQtyByVariantId.getOrDefault(variantId, 0);
            int availableForNewOrder = stock - getActiveReservedQuantity(variantId);
            if (availableForNewOrder < requested) {
                throw new BusinessException("Not enough stock for variant: " + lockedVariant.getSku(), HttpStatus.BAD_REQUEST);
            }
            lockedVariants.put(variantId, lockedVariant);
        }

        OrderEntity order = new OrderEntity();
        order.setUserId(user.getId());
        order.setSubTotal(subTotal);
        order.setShippingFee(shippingFee);
        order.setDiscountAmount(discountAmount);
        order.setCouponId(resolvedCoupon == null ? null : resolvedCoupon.coupon.getId());
        order.setCouponCode(resolvedCoupon == null ? null : resolvedCoupon.coupon.getCode());
        order.setTotalPrice(total);
        order.setStatus(STATUS_WAITING_PAYMENT);
        order.setPaymentMethod(PAYMENT_MOMO);
        order.setShippingProvider(SHIPPING_PROVIDER_GHN);
        order.setAddress(safeTrim(request.getAddress()));
        order.setCreatedAt(LocalDateTime.now());
        OrderEntity savedOrder = orderRepository.save(order);

        for (CartItemEntity item : cartItems) {
            ProductVariantEntity variant = lockedVariants.get(item.getVariantId());
            if (variant == null) {
                throw new BusinessException("Variant not found", HttpStatus.BAD_REQUEST);
            }
            reserveStockForMomoOrder(savedOrder, variant.getId(), item.getQuantity(), "Reserve for MoMo client waiting payment order #");

            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setVariantId(variant.getId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(variant.getPrice());
            orderItemRepository.save(orderItem);
        }

        session.setToken(token);
        session.setUserId(user.getId());
        session.setRequestPayload(writeJson(request));
        session.setCartItemsPayload(writeJson(toMomoCartSnapshot(cartItems)));
        session.setSubTotal(subTotal);
        session.setShippingFee(shippingFee);
        session.setDiscountAmount(discountAmount);
        session.setTotalPrice(total);
        session.setCouponId(resolvedCoupon == null ? null : resolvedCoupon.coupon.getId());
        session.setCouponCode(resolvedCoupon == null ? null : resolvedCoupon.coupon.getCode());
        session.setStatus(MOMO_SESSION_STATUS_PENDING);
        session.setCreatedOrderId(savedOrder.getId());
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(MOMO_SESSION_TTL_MINUTES));
        momoCheckoutSessionRepository.save(session);

        String payUrl;
        try {
            String extraDataRaw = "sessionToken=" + token;
            String extraData = Base64.getEncoder().encodeToString(extraDataRaw.getBytes(StandardCharsets.UTF_8));
            payUrl = paymentService.createMomoPaymentForSession(
                    token,
                    total,
                    "Thanh toan don hang tam",
                    extraData,
                    request.getMomoRequestType()
            );
        } catch (RuntimeException ex) {
            momoCheckoutSessionRepository.delete(session);
            orderRepository.delete(savedOrder);
            throw ex;
        }

        addHistory(savedOrder.getId(), STATUS_WAITING_PAYMENT);
        return toResponseWithPaymentUrl(savedOrder, payUrl);
    }

    private OrderResponse createVnpayCheckoutSession(
            UserEntity user,
            CreateOrderRequest request,
            List<CartItemEntity> cartItems,
            long subTotal,
            long shippingFee,
            long discountAmount,
            long total,
            ResolvedCoupon resolvedCoupon
    ) {
        if (!ghnShippingService.canCallShippingApi()) {
            throw new BusinessException("GHN shipping config is incomplete", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        ReceiverInfo receiverInfo = resolveReceiverInfo(user, request);
        if (receiverInfo.address.isBlank()
                || receiverInfo.district.isBlank()
                || receiverInfo.ward.isBlank()
                || receiverInfo.phone.isBlank()) {
            throw new BusinessException(
                    "Thiếu thông tin nhận hàng để tạo đơn GHN (address/district/ward/phone)",
                    HttpStatus.BAD_REQUEST
            );
        }
        if (!isValidShippingPhone(receiverInfo.phone)) {
            throw new BusinessException(
                    "Số điện thoại nhận hàng không hợp lệ. Vui lòng nhập đúng 10 số (VD: 09xxxxxxxx)",
                    HttpStatus.BAD_REQUEST
            );
        }

        VnpayCheckoutSessionEntity session = new VnpayCheckoutSessionEntity();
        String token = UUID.randomUUID().toString().replace("-", "");
        session.setToken(token);
        session.setUserId(user.getId());
        session.setRequestPayload(writeJson(request));
        session.setCartItemsPayload(writeJson(toMomoCartSnapshot(cartItems)));
        session.setSubTotal(subTotal);
        session.setShippingFee(shippingFee);
        session.setDiscountAmount(discountAmount);
        session.setTotalPrice(total);
        session.setCouponId(resolvedCoupon == null ? null : resolvedCoupon.coupon.getId());
        session.setCouponCode(resolvedCoupon == null ? null : resolvedCoupon.coupon.getCode());
        session.setStatus(VNPAY_SESSION_STATUS_PENDING);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(VNPAY_SESSION_TTL_MINUTES));
        vnpayCheckoutSessionRepository.save(session);

        String payUrl;
        try {
            payUrl = paymentService.createVnpayPaymentForSession(
                    token,
                    total,
                    "Thanh toan don hang tam",
                    request.getVnpayBankCode()
            );
        } catch (RuntimeException ex) {
            vnpayCheckoutSessionRepository.delete(session);
            throw ex;
        }

        return OrderResponse.builder()
                .id(null)
                .userId(user.getId())
                .totalPrice(total)
                .subTotal(subTotal)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .appliedVoucherCode(session.getCouponCode())
                .status("WAITING_PAYMENT")
                .paymentMethod(PAYMENT_VNPAY)
                .paymentUrl(payUrl)
                .address(safeTrim(request.getAddress()))
                .createdAt(session.getCreatedAt())
                .items(List.of())
                .statusHistory(List.of())
                .build();
    }

    private OrderEntity createOrderFromMomoSession(
            MomoCheckoutSessionEntity session,
            UserEntity user,
            CreateOrderRequest request,
            List<MomoCartItemSnapshot> snapshots
    ) {
        OrderEntity order = new OrderEntity();
        order.setUserId(session.getUserId());
        order.setSubTotal(session.getSubTotal());
        order.setShippingFee(session.getShippingFee());
        order.setDiscountAmount(session.getDiscountAmount());
        order.setCouponId(session.getCouponId());
        order.setCouponCode(session.getCouponCode());
        order.setTotalPrice(session.getTotalPrice());
        order.setStatus(STATUS_PENDING);
        order.setPaymentMethod(PAYMENT_MOMO);
        order.setShippingProvider(SHIPPING_PROVIDER_GHN);
        order.setAddress(safeTrim(request.getAddress()));
        order.setCreatedAt(LocalDateTime.now());
        OrderEntity savedOrder = orderRepository.save(order);

        List<CartItemEntity> pseudoCartItems = new ArrayList<>();
        for (MomoCartItemSnapshot snapshot : snapshots) {
            Long variantId = snapshot.variantId == null ? null : snapshot.variantId;
            Integer quantity = snapshot.quantity == null ? null : snapshot.quantity;
            Long unitPrice = snapshot.unitPrice == null ? null : snapshot.unitPrice;
            if (variantId == null || quantity == null || quantity <= 0) {
                continue;
            }
            getVariant(variantId);

            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setVariantId(variantId);
            orderItem.setQuantity(quantity);
            orderItem.setPrice(unitPrice == null || unitPrice < 0 ? 0L : unitPrice);
            orderItemRepository.save(orderItem);

            CartItemEntity pseudoCartItem = new CartItemEntity();
            pseudoCartItem.setVariantId(variantId);
            pseudoCartItem.setQuantity(quantity);
            pseudoCartItems.add(pseudoCartItem);
        }
        if (pseudoCartItems.isEmpty()) {
            throw new BusinessException("MoMo session has no valid items", HttpStatus.BAD_REQUEST);
        }

        try {
            tryCreateGhnShippingOrder(savedOrder, user, request, pseudoCartItems);
        } catch (Exception ex) {
            log.error("Cannot create GHN shipping order after MoMo paid. orderId={}", savedOrder.getId(), ex);
            savedOrder.setShippingStatus("CREATE_FAILED");
            savedOrder.setShippingUpdatedAt(LocalDateTime.now());
            orderRepository.save(savedOrder);
        }

        addHistory(savedOrder.getId(), STATUS_PENDING);

        if (session.getCouponId() != null) {
            CouponEntity coupon = couponRepository.findById(session.getCouponId()).orElse(null);
            if (coupon != null) {
                coupon.setUsedCount((coupon.getUsedCount() == null ? 0 : coupon.getUsedCount()) + 1);
                couponRepository.save(coupon);

                CouponUsageEntity usage = new CouponUsageEntity();
                usage.setCouponId(coupon.getId());
                usage.setUserId(savedOrder.getUserId());
                usage.setOrderId(savedOrder.getId());
                usage.setUsedAt(LocalDateTime.now());
                couponUsageRepository.save(usage);
            }
        }

        CartEntity cart = cartRepository.findByUserId(savedOrder.getUserId()).orElse(null);
        if (cart != null) {
            cartItemRepository.deleteByCartId(cart.getId());
        }

        publishOrderCreatedAfterCommit(savedOrder.getId());
        auditLogService.log("ORDER_CREATED", "ORDER", savedOrder.getId(), "Created order after MoMo payment");
        return savedOrder;
    }

    private OrderEntity createOrderFromVnpaySession(
            VnpayCheckoutSessionEntity session,
            UserEntity user,
            CreateOrderRequest request,
            List<MomoCartItemSnapshot> snapshots
    ) {
        OrderEntity order = new OrderEntity();
        order.setUserId(session.getUserId());
        order.setSubTotal(session.getSubTotal());
        order.setShippingFee(session.getShippingFee());
        order.setDiscountAmount(session.getDiscountAmount());
        order.setCouponId(session.getCouponId());
        order.setCouponCode(session.getCouponCode());
        order.setTotalPrice(session.getTotalPrice());
        order.setStatus(STATUS_PENDING);
        order.setPaymentMethod(PAYMENT_VNPAY);
        order.setShippingProvider(SHIPPING_PROVIDER_GHN);
        order.setAddress(safeTrim(request.getAddress()));
        order.setCreatedAt(LocalDateTime.now());
        OrderEntity savedOrder = orderRepository.save(order);

        List<CartItemEntity> pseudoCartItems = new ArrayList<>();
        for (MomoCartItemSnapshot snapshot : snapshots) {
            Long variantId = snapshot.variantId == null ? null : snapshot.variantId;
            Integer quantity = snapshot.quantity == null ? null : snapshot.quantity;
            Long unitPrice = snapshot.unitPrice == null ? null : snapshot.unitPrice;
            if (variantId == null || quantity == null || quantity <= 0) {
                continue;
            }
            getVariant(variantId);

            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setVariantId(variantId);
            orderItem.setQuantity(quantity);
            orderItem.setPrice(unitPrice == null || unitPrice < 0 ? 0L : unitPrice);
            orderItemRepository.save(orderItem);

            CartItemEntity pseudoCartItem = new CartItemEntity();
            pseudoCartItem.setVariantId(variantId);
            pseudoCartItem.setQuantity(quantity);
            pseudoCartItems.add(pseudoCartItem);
        }
        if (pseudoCartItems.isEmpty()) {
            throw new BusinessException("VNPAY session has no valid items", HttpStatus.BAD_REQUEST);
        }

        try {
            tryCreateGhnShippingOrder(savedOrder, user, request, pseudoCartItems);
        } catch (Exception ex) {
            log.error("Cannot create GHN shipping order after VNPAY paid. orderId={}", savedOrder.getId(), ex);
            savedOrder.setShippingStatus("CREATE_FAILED");
            savedOrder.setShippingUpdatedAt(LocalDateTime.now());
            orderRepository.save(savedOrder);
        }

        addHistory(savedOrder.getId(), STATUS_PENDING);

        if (session.getCouponId() != null) {
            CouponEntity coupon = couponRepository.findById(session.getCouponId()).orElse(null);
            if (coupon != null) {
                coupon.setUsedCount((coupon.getUsedCount() == null ? 0 : coupon.getUsedCount()) + 1);
                couponRepository.save(coupon);

                CouponUsageEntity usage = new CouponUsageEntity();
                usage.setCouponId(coupon.getId());
                usage.setUserId(savedOrder.getUserId());
                usage.setOrderId(savedOrder.getId());
                usage.setUsedAt(LocalDateTime.now());
                couponUsageRepository.save(usage);
            }
        }

        CartEntity cart = cartRepository.findByUserId(savedOrder.getUserId()).orElse(null);
        if (cart != null) {
            cartItemRepository.deleteByCartId(cart.getId());
        }

        publishOrderCreatedAfterCommit(savedOrder.getId());
        auditLogService.log("ORDER_CREATED", "ORDER", savedOrder.getId(), "Created order after VNPAY payment");
        return savedOrder;
    }

    private List<MomoCartItemSnapshot> toMomoCartSnapshot(List<CartItemEntity> cartItems) {
        List<MomoCartItemSnapshot> snapshots = new ArrayList<>();
        for (CartItemEntity cartItem : cartItems) {
            ProductVariantEntity variant = getVariant(cartItem.getVariantId());
            int quantity = cartItem.getQuantity() == null ? 0 : cartItem.getQuantity();
            if (quantity <= 0) {
                continue;
            }
            MomoCartItemSnapshot snapshot = new MomoCartItemSnapshot();
            snapshot.variantId = variant.getId();
            snapshot.quantity = quantity;
            snapshot.unitPrice = variant.getPrice() == null ? 0L : variant.getPrice();
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    private List<MomoCartItemSnapshot> parseCartSnapshot(String payload) {
        if (payload == null || payload.isBlank()) {
            return List.of();
        }
        try {
            List<MomoCartItemSnapshot> parsed = objectMapper.readValue(payload, new TypeReference<List<MomoCartItemSnapshot>>() {});
            return parsed == null ? List.of() : parsed;
        } catch (Exception ex) {
            throw new BusinessException("Invalid MoMo cart snapshot payload", HttpStatus.BAD_REQUEST);
        }
    }

    private CreateOrderRequest parseCreateOrderRequestSnapshot(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new BusinessException("Missing MoMo request snapshot", HttpStatus.BAD_REQUEST);
        }
        try {
            CreateOrderRequest request = objectMapper.readValue(payload, CreateOrderRequest.class);
            if (request == null) {
                throw new BusinessException("Missing MoMo request snapshot", HttpStatus.BAD_REQUEST);
            }
            return request;
        } catch (Exception ex) {
            throw new BusinessException("Invalid MoMo request snapshot payload", HttpStatus.BAD_REQUEST);
        }
    }

    private String extractMomoSessionToken(Object extraDataObj) {
        String encoded = toText(extraDataObj);
        if (encoded.isBlank()) {
            return "";
        }
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            decoded = encoded;
        }
        String prefix = "sessionToken=";
        if (decoded.startsWith(prefix)) {
            return safeTrim(decoded.substring(prefix.length()));
        }
        for (String segment : decoded.split("&")) {
            if (segment.startsWith(prefix)) {
                return safeTrim(segment.substring(prefix.length()));
            }
        }
        return "";
    }

    private String extractVnpaySessionTokenFromTxnRef(String txnRef) {
        String value = safeTrim(txnRef);
        if (value.isBlank() || !value.startsWith("TMP")) {
            return "";
        }
        int lastDash = value.lastIndexOf('-');
        if (lastDash <= 3) {
            return "";
        }
        return safeTrim(value.substring(3, lastDash));
    }

    private int parseInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private int getActiveReservedQuantity(Long variantId) {
        if (variantId == null) {
            return 0;
        }
        long reserved = stockReservationRepository.sumActiveQuantityByVariantId(
                variantId,
                RESERVATION_STATUS_ACTIVE,
                LocalDateTime.now()
        );
        if (reserved <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, reserved);
    }

    private void reserveStockForMomoPosOrder(OrderEntity order, Long variantId, Integer quantityValue) {
        reserveStockForMomoOrder(order, variantId, quantityValue, "Reserve for MoMo POS waiting payment order #");
    }

    private void reserveStockForCodOrder(OrderEntity order, Long variantId, Integer quantityValue) {
        reserveStockForOrder(order, variantId, quantityValue, "Reserve for COD order #", COD_RESERVATION_TTL_MINUTES);
    }

    private void reserveStockForMomoOrder(
            OrderEntity order,
            Long variantId,
            Integer quantityValue,
            String notePrefix
    ) {
        reserveStockForOrder(order, variantId, quantityValue, notePrefix, MOMO_SESSION_TTL_MINUTES);
    }

    private void reserveStockForOrder(
            OrderEntity order,
            Long variantId,
            Integer quantityValue,
            String notePrefix,
            long ttlMinutes
    ) {
        if (order == null || order.getId() == null || variantId == null) {
            return;
        }
        int quantity = quantityValue == null ? 0 : quantityValue;
        if (quantity <= 0) {
            return;
        }
        StockReservationEntity reservation = new StockReservationEntity();
        reservation.setOrderId(order.getId());
        reservation.setVariantId(variantId);
        reservation.setQuantity(quantity);
        reservation.setStatus(RESERVATION_STATUS_ACTIVE);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(Math.max(1L, ttlMinutes)));
        reservation.setNote(safeTrim(notePrefix) + order.getId());
        reservation.setCreatedAt(LocalDateTime.now());
        stockReservationRepository.save(reservation);
    }

    private List<CartItemEntity> toPseudoCartItems(List<MomoCartItemSnapshot> snapshots) {
        List<CartItemEntity> pseudoCartItems = new ArrayList<>();
        for (MomoCartItemSnapshot snapshot : snapshots) {
            Long variantId = snapshot.variantId == null ? null : snapshot.variantId;
            Integer quantity = snapshot.quantity == null ? null : snapshot.quantity;
            if (variantId == null || quantity == null || quantity <= 0) {
                continue;
            }
            CartItemEntity pseudoCartItem = new CartItemEntity();
            pseudoCartItem.setVariantId(variantId);
            pseudoCartItem.setQuantity(quantity);
            pseudoCartItems.add(pseudoCartItem);
        }
        if (pseudoCartItems.isEmpty()) {
            throw new BusinessException("MoMo session has no valid items", HttpStatus.BAD_REQUEST);
        }
        return pseudoCartItems;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new BusinessException("Cannot serialize checkout snapshot", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private List<ProductSalesStatResponse> buildTopProducts() {
        List<TopProductSalesProjection> topRows = orderItemRepository.findTopProductSales30dDelivered();
        if (topRows.isEmpty()) {
            return List.of();
        }

        List<ProductSalesStatResponse> responses = new ArrayList<>(topRows.size());
        for (TopProductSalesProjection row : topRows) {
            Long productId = row.getProductId();
            String productName = row.getProductName();
            responses.add(ProductSalesStatResponse.builder()
                    .productId(productId)
                    .productName(productName == null || productName.isBlank() ? ("Product #" + productId) : productName)
                    .totalQuantity(safeLong(row.getTotalQuantity()))
                    .totalRevenue(safeLong(row.getTotalRevenue()))
                    .build());
        }
        return responses;
    }

    private String normalizeStatus(String status) {
        return String.valueOf(status == null ? "" : status).trim().toUpperCase(Locale.ROOT);
    }

    private List<OrderResponse> fetchInvoicesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("ids must not be empty", HttpStatus.BAD_REQUEST);
        }
        List<Long> normalizedIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedIds.isEmpty()) {
            throw new BusinessException("ids must not be empty", HttpStatus.BAD_REQUEST);
        }

        List<OrderEntity> foundOrders = orderRepository.findAllById(normalizedIds);
        if (foundOrders.isEmpty()) {
            throw new BusinessException("Orders not found", HttpStatus.NOT_FOUND);
        }

        Set<Long> foundIds = foundOrders.stream()
                .map(OrderEntity::getId)
                .collect(Collectors.toSet());
        List<Long> missingIds = normalizedIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();
        if (!missingIds.isEmpty()) {
            throw new BusinessException("Orders not found: " + missingIds, HttpStatus.NOT_FOUND);
        }

        Map<Long, Integer> orderIndex = new HashMap<>();
        for (int i = 0; i < normalizedIds.size(); i++) {
            orderIndex.put(normalizedIds.get(i), i);
        }
        foundOrders.sort(Comparator.comparingInt(order -> orderIndex.getOrDefault(order.getId(), Integer.MAX_VALUE)));
        return toResponses(foundOrders);
    }

    private String nonNullText(String value, String fallback) {
        String trimmed = safeTrim(value);
        return trimmed.isBlank() ? fallback : trimmed;
    }

    private String normalizeShippingCode(String shippingCode) {
        if (shippingCode == null) {
            return "";
        }
        return shippingCode.trim();
    }

    private String toText(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
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
            case STATUS_WAITING_PAYMENT -> Set.of(STATUS_DELIVERED, "CONFIRMED", "CANCELLED", "FAILED").contains(targetStatus);
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
        if (shouldCaptureReservedStockOnStatusTransition(order, current, normalized)) {
            captureReservedStockForMomoPosOrder(order);
        } else if (shouldReleaseReservedStockOnStatusTransition(order, current, normalized)) {
            releaseReservedStockForMomoPosOrder(order);
        }
        order.setStatus(normalized);
        if (externalSync) {
            order.setShippingUpdatedAt(LocalDateTime.now());
        }
        orderRepository.save(order);
        addHistory(order.getId(), normalized);
    }

    private boolean shouldCaptureReservedStockOnStatusTransition(OrderEntity order, String currentStatus, String targetStatus) {
        if (order == null) {
            return false;
        }
        String provider = safeTrim(order.getShippingProvider()).toUpperCase(Locale.ROOT);
        String paymentMethod = safeTrim(order.getPaymentMethod()).toUpperCase(Locale.ROOT);

        if (PAYMENT_MOMO.equals(paymentMethod) && STATUS_WAITING_PAYMENT.equals(currentStatus)) {
            boolean posCaptureTarget = Set.of("CONFIRMED", STATUS_DELIVERED).contains(targetStatus);
            boolean clientCaptureTarget = SHIPPING_PROVIDER_GHN.equals(provider) && STATUS_PENDING.equals(targetStatus);
            if (posCaptureTarget || clientCaptureTarget) {
                return stockReservationRepository.existsByOrderIdAndStatusIgnoreCase(order.getId(), RESERVATION_STATUS_ACTIVE);
            }
            return false;
        }

        if (PAYMENT_COD.equals(paymentMethod)
                && SHIPPING_PROVIDER_GHN.equals(provider)
                && Set.of(STATUS_PENDING, "PROCESSING", "CONFIRMED").contains(currentStatus)
                && Set.of("SHIPPED", STATUS_DELIVERED).contains(targetStatus)) {
            return stockReservationRepository.existsByOrderIdAndStatusIgnoreCase(order.getId(), RESERVATION_STATUS_ACTIVE);
        }
        return false;
    }

    private boolean shouldReleaseReservedStockOnStatusTransition(OrderEntity order, String currentStatus, String targetStatus) {
        if (order == null) {
            return false;
        }
        if (!Set.of(STATUS_CANCELLED, STATUS_FAILED, "FAILED_DELIVERY").contains(targetStatus)) {
            return false;
        }
        String provider = safeTrim(order.getShippingProvider()).toUpperCase(Locale.ROOT);
        String paymentMethod = safeTrim(order.getPaymentMethod()).toUpperCase(Locale.ROOT);
        if (PAYMENT_MOMO.equals(paymentMethod) && STATUS_WAITING_PAYMENT.equals(currentStatus)) {
            return stockReservationRepository.existsByOrderIdAndStatusIgnoreCase(order.getId(), RESERVATION_STATUS_ACTIVE);
        }
        if (PAYMENT_COD.equals(paymentMethod)
                && SHIPPING_PROVIDER_GHN.equals(provider)
                && Set.of(STATUS_PENDING, "PROCESSING", "CONFIRMED").contains(currentStatus)) {
            return stockReservationRepository.existsByOrderIdAndStatusIgnoreCase(order.getId(), RESERVATION_STATUS_ACTIVE);
        }
        if (!PAYMENT_MOMO.equals(paymentMethod)) {
            return false;
        }
        return stockReservationRepository.existsByOrderIdAndStatusIgnoreCase(order.getId(), RESERVATION_STATUS_ACTIVE);
    }

    private void captureReservedStockForMomoPosOrder(OrderEntity order) {
        List<StockReservationEntity> reservations = stockReservationRepository
                .findByOrderIdAndStatusIgnoreCaseOrderByIdAsc(order.getId(), RESERVATION_STATUS_ACTIVE);
        if (reservations.isEmpty()) {
            return;
        }
        for (StockReservationEntity reservation : reservations) {
            int quantity = reservation.getQuantity() == null ? 0 : reservation.getQuantity();
            if (quantity <= 0) {
                reservation.setStatus(RESERVATION_STATUS_CONVERTED);
                reservation.setUpdatedAt(LocalDateTime.now());
                continue;
            }
            inventoryMovementService.deductStockByVariantId(
                    reservation.getVariantId(),
                    quantity,
                    resolveCaptureInventoryType(order),
                    "Capture reserved stock for MoMo waiting payment order #" + order.getId()
            );
            reservation.setStatus(RESERVATION_STATUS_CONVERTED);
            reservation.setUpdatedAt(LocalDateTime.now());
        }
        stockReservationRepository.saveAll(reservations);
    }

    private void releaseReservedStockForMomoPosOrder(OrderEntity order) {
        List<StockReservationEntity> reservations = stockReservationRepository
                .findByOrderIdAndStatusIgnoreCaseOrderByIdAsc(order.getId(), RESERVATION_STATUS_ACTIVE);
        if (!reservations.isEmpty()) {
            for (StockReservationEntity reservation : reservations) {
                reservation.setStatus(RESERVATION_STATUS_RELEASED);
                reservation.setUpdatedAt(LocalDateTime.now());
            }
            stockReservationRepository.saveAll(reservations);
            return;
        }

        // Legacy fallback: old WAITING_PAYMENT MoMo POS orders may have been deducted immediately.
        String provider = safeTrim(order.getShippingProvider()).toUpperCase(Locale.ROOT);
        if (provider.startsWith("POS_")) {
            List<OrderItemEntity> orderItems = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
            for (OrderItemEntity item : orderItems) {
                int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
                if (quantity <= 0) {
                    continue;
                }
                ProductVariantEntity variant = getVariant(item.getVariantId());
                inventoryMovementService.increaseStockBySku(
                        variant.getSku(),
                        quantity,
                        INVENTORY_TYPE_POS_REVERT,
                        "Restore stock for legacy cancelled/failed MoMo POS order #" + order.getId()
                );
            }
        }
    }

    private String resolveCaptureInventoryType(OrderEntity order) {
        String provider = safeTrim(order == null ? null : order.getShippingProvider()).toUpperCase(Locale.ROOT);
        String paymentMethod = safeTrim(order == null ? null : order.getPaymentMethod()).toUpperCase(Locale.ROOT);
        if (PAYMENT_COD.equals(paymentMethod) && SHIPPING_PROVIDER_GHN.equals(provider)) {
            return INVENTORY_TYPE_COD_CAPTURE;
        }
        return provider.startsWith("POS_") ? INVENTORY_TYPE_POS_CAPTURE : INVENTORY_TYPE_CLIENT_CAPTURE;
    }

    private OrderResponse toResponse(OrderEntity order) {
        return toResponses(List.of(order)).get(0);
    }

    private OrderResponse toResponseWithPaymentUrl(OrderEntity order, String paymentUrl) {
        OrderResponse base = toResponse(order);
        return OrderResponse.builder()
                .id(base.getId())
                .userId(base.getUserId())
                .customerName(base.getCustomerName())
                .totalPrice(base.getTotalPrice())
                .subTotal(base.getSubTotal())
                .shippingFee(base.getShippingFee())
                .discountAmount(base.getDiscountAmount())
                .appliedVoucherCode(base.getAppliedVoucherCode())
                .status(base.getStatus())
                .paymentMethod(base.getPaymentMethod())
                .paymentUrl(paymentUrl)
                .shippingProvider(base.getShippingProvider())
                .shippingCode(base.getShippingCode())
                .shippingStatus(base.getShippingStatus())
                .shippingUpdatedAt(base.getShippingUpdatedAt())
                .address(base.getAddress())
                .note(base.getNote())
                .createdAt(base.getCreatedAt())
                .items(base.getItems())
                .statusHistory(base.getStatusHistory())
                .build();
    }

    private List<OrderResponse> toResponses(List<OrderEntity> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream()
                .map(OrderEntity::getId)
                .filter(id -> id != null && id > 0)
                .toList();

        Set<Long> userIds = orders.stream()
                .map(OrderEntity::getUserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, UserEntity> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, user -> user));

        List<OrderItemEntity> allItems = orderItemRepository.findByOrderIdInOrderByOrderIdAscIdAsc(orderIds);
        Map<Long, List<OrderItemEntity>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(OrderItemEntity::getOrderId));

        Set<Long> variantIds = allItems.stream()
                .map(OrderItemEntity::getVariantId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, ProductVariantEntity> variantById = productVariantRepository.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductVariantEntity::getId, variant -> variant));

        Set<Long> productIds = variantById.values().stream()
                .map(ProductVariantEntity::getProductId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, ProductEntity> productById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(ProductEntity::getId, product -> product));

        List<OrderStatusHistoryEntity> allHistory = orderStatusHistoryRepository.findByOrderIdInOrderByOrderIdAscIdAsc(orderIds);
        Map<Long, List<OrderStatusHistoryEntity>> historyByOrderId = allHistory.stream()
                .collect(Collectors.groupingBy(OrderStatusHistoryEntity::getOrderId));

        List<OrderResponse> responses = new ArrayList<>(orders.size());
        for (OrderEntity order : orders) {
            UserEntity customer = usersById.get(order.getUserId());
            String customerName = customer == null
                    ? null
                    : (customer.getFullName() != null && !customer.getFullName().isBlank()
                        ? customer.getFullName().trim()
                        : customer.getUsername());
            if (isPosGuestOrder(order)) {
                customerName = "Khách lẻ";
            }

            List<OrderItemResponse> itemResponses = itemsByOrderId.getOrDefault(order.getId(), List.of()).stream()
                    .map(item -> {
                        ProductVariantEntity variant = variantById.get(item.getVariantId());
                        ProductEntity product = variant == null ? null : productById.get(variant.getProductId());
                        long safePrice = item.getPrice() == null ? 0L : item.getPrice();
                        int safeQty = item.getQuantity() == null ? 0 : item.getQuantity();
                        return OrderItemResponse.builder()
                                .id(item.getId())
                                .productId(product == null ? null : product.getId())
                                .variantId(item.getVariantId())
                                .sku(variant == null ? null : variant.getSku())
                                .productName(product == null ? null : product.getName())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .lineTotal(safePrice * safeQty)
                                .build();
                    })
                    .toList();

            List<OrderStatusHistoryResponse> historyResponses = historyByOrderId.getOrDefault(order.getId(), List.of())
                    .stream()
                    .map(h -> OrderStatusHistoryResponse.builder()
                            .status(h.getStatus())
                            .changedAt(h.getChangedAt())
                            .build())
                    .toList();

            responses.add(OrderResponse.builder()
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
                    .note(order.getNote())
                    .createdAt(order.getCreatedAt())
                    .items(itemResponses)
                    .statusHistory(historyResponses)
                    .build());
        }
        return responses;
    }

    private boolean isPosGuestOrder(OrderEntity order) {
        if (order == null) return false;
        String provider = safeTrim(order.getShippingProvider()).toUpperCase(Locale.ROOT);
        if (!provider.startsWith("POS_")) {
            return false;
        }
        String address = safeTrim(order.getAddress()).toLowerCase(Locale.ROOT);
        return address.contains("khach: khach le") || address.contains("khách: khách lẻ");
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
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
        try {
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
                log.warn("GHN returned empty shipping code for order {}", order.getId());
                order.setShippingStatus("pending_create");
                order.setShippingUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                return;
            }

            order.setShippingCode(shippingCode);
            order.setShippingStatus("ready_to_pick");
            order.setShippingUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        } catch (BusinessException ex) {
            // GHN temporary/system errors should not block checkout.
            if (ex.getStatus() == HttpStatus.BAD_GATEWAY || ex.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR) {
                log.warn("Skip GHN create for order {} due to upstream error: {}", order.getId(), ex.getMessage());
                order.setShippingStatus("pending_create");
                order.setShippingUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                return;
            }
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected GHN create error for order {}: {}", order.getId(), ex.getMessage(), ex);
            order.setShippingStatus("pending_create");
            order.setShippingUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        }
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

    private void validatePosShippingFields(PosCheckoutRequest request) {
        if (safeTrim(request.getRecipientName()).isBlank()) {
            throw new BusinessException("recipientName is required for shipping", HttpStatus.BAD_REQUEST);
        }
        if (safeTrim(request.getPhone()).isBlank()) {
            throw new BusinessException("phone is required for shipping", HttpStatus.BAD_REQUEST);
        }
        if (safeTrim(request.getAddress()).isBlank()) {
            throw new BusinessException("address is required for shipping", HttpStatus.BAD_REQUEST);
        }
    }

    private String buildPosAddress(PosCheckoutRequest request, UserEntity customer) {
        if (!Boolean.TRUE.equals(request.getShipEnabled())) {
            String customerName = customer == null
                    ? "Khach le"
                    : firstNonBlank(customer.getFullName(), customer.getUsername(), "Khach le");
            return "BAN_TAI_QUAY | Khach: " + customerName;
        }

        return String.format(
                "SHIP | Nguoi nhan: %s | SDT: %s | Dia chi: %s | Ward: %s | District: %s | Province: %s",
                safeTrim(request.getRecipientName()),
                safeTrim(request.getPhone()),
                safeTrim(request.getAddress()),
                safeTrim(request.getWard()),
                safeTrim(request.getDistrict()),
                safeTrim(request.getProvince())
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

    private long resolveShippingFee(CreateOrderRequest request, long subTotal, String province) {
        Long requestShippingFee = request.getShippingFee();
        if (requestShippingFee != null && requestShippingFee >= 0) {
            return requestShippingFee;
        }
        return calculateShippingFee(subTotal, province);
    }

    private ResolvedCoupon resolveBestCoupon(Long userId, long subTotal, String preferredCode) {
        LocalDateTime now = LocalDateTime.now();
        List<CouponEntity> candidates = couponRepository.findAll().stream()
                .filter(coupon -> "ACTIVE".equalsIgnoreCase(String.valueOf(coupon.getStatus())))
                .filter(coupon -> coupon.getStartDate() == null || !coupon.getStartDate().isAfter(now))
                .filter(coupon -> coupon.getEndDate() == null || !coupon.getEndDate().isBefore(now))
                .filter(coupon -> coupon.getMinOrderValue() == null || subTotal >= coupon.getMinOrderValue())
                .filter(coupon -> coupon.getQuantity() == null || (coupon.getUsedCount() == null ? 0 : coupon.getUsedCount()) < coupon.getQuantity())
                .filter(coupon -> userId == null || !couponUsageRepository.existsByCouponIdAndUserId(coupon.getId(), userId))
                .toList();

        if (preferredCode != null && !preferredCode.isBlank()) {
            String normalized = preferredCode.trim().toUpperCase(Locale.ROOT);
            CouponEntity preferred = candidates.stream()
                    .filter(coupon -> normalized.equalsIgnoreCase(coupon.getCode()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Voucher không hợp lệ hoặc không áp dụng được", HttpStatus.BAD_REQUEST));
            long discount = calculateCouponDiscount(preferred, subTotal);
            if (discount <= 0) {
                throw new BusinessException("Voucher không tạo được giảm giá", HttpStatus.BAD_REQUEST);
            }
            return new ResolvedCoupon(preferred, discount);
        }
        if (candidates.isEmpty()) return null;

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

    private static class MomoCartItemSnapshot {
        public Long variantId;
        public Integer quantity;
        public Long unitPrice;
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
