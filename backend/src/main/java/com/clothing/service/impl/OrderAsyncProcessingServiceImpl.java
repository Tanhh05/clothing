package com.clothing.service.impl;

import com.clothing.entity.NotificationEntity;
import com.clothing.entity.OrderEntity;
import com.clothing.entity.OrderItemEntity;
import com.clothing.entity.OrderStatusHistoryEntity;
import com.clothing.entity.ProductVariantEntity;
import com.clothing.entity.UserEntity;
import com.clothing.repository.NotificationRepository;
import com.clothing.repository.OrderItemRepository;
import com.clothing.repository.OrderRepository;
import com.clothing.repository.OrderStatusHistoryRepository;
import com.clothing.repository.ProductVariantRepository;
import com.clothing.repository.StockReservationRepository;
import com.clothing.repository.UserRepository;
import com.clothing.service.EmailService;
import com.clothing.service.InventoryMovementService;
import com.clothing.service.OrderAsyncProcessingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderAsyncProcessingServiceImpl implements OrderAsyncProcessingService {

    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_FAILED_STOCK = "FAILED_INSUFFICIENT_STOCK";
    private static final String TYPE_DEDUCT = "DEDUCT";
    private static final String RESERVATION_STATUS_ACTIVE = "ACTIVE";
    private static final String RESERVATION_STATUS_CONVERTED = "CONVERTED";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final StockReservationRepository stockReservationRepository;
    private final InventoryMovementService inventoryMovementService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public OrderAsyncProcessingServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            ProductVariantRepository productVariantRepository,
            StockReservationRepository stockReservationRepository,
            InventoryMovementService inventoryMovementService,
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            EmailService emailService
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.productVariantRepository = productVariantRepository;
        this.stockReservationRepository = stockReservationRepository;
        this.inventoryMovementService = inventoryMovementService;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public void processOrderCreated(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        if (STATUS_CONFIRMED.equalsIgnoreCase(order.getStatus())
                || STATUS_FAILED.equalsIgnoreCase(order.getStatus())
                || STATUS_FAILED_STOCK.equalsIgnoreCase(order.getStatus())) {
            return;
        }

        setStatus(order, STATUS_PROCESSING);
        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderIdOrderByIdAsc(orderId);
        boolean hasActiveReservation = stockReservationRepository
                .existsByOrderIdAndStatusIgnoreCase(orderId, RESERVATION_STATUS_ACTIVE);
        boolean alreadyCapturedReservation = stockReservationRepository
                .existsByOrderIdAndStatusIgnoreCase(orderId, RESERVATION_STATUS_CONVERTED);
        boolean skipStockDeduct = hasActiveReservation || alreadyCapturedReservation;

        String failureReason = null;
        if (!skipStockDeduct) {
            for (OrderItemEntity item : orderItems) {
                ProductVariantEntity variant = productVariantRepository.findByIdForUpdate(item.getVariantId())
                        .orElse(null);
                if (variant == null) {
                    failureReason = "Variant not found: " + item.getVariantId();
                    break;
                }
                if (variant.getStock() == null || variant.getStock() < item.getQuantity()) {
                    failureReason = "Not enough stock for SKU: " + variant.getSku();
                    break;
                }
            }
        }

        UserEntity user = userRepository.findById(order.getUserId()).orElse(null);
        String userEmail = user != null ? user.getEmail() : null;

        if (failureReason != null) {
            setStatus(order, STATUS_FAILED_STOCK);
            createNotification(order.getUserId(), "Đơn hàng xử lý thất bại", failureReason, "ORDER_FAILED");
            if (userEmail != null && !userEmail.isBlank()) {
                emailService.sendOrderFailedEmail(userEmail, order.getId(), failureReason);
            }
            return;
        }

        if (!skipStockDeduct) {
            for (OrderItemEntity item : orderItems) {
                inventoryMovementService.deductStockByVariantId(
                        item.getVariantId(),
                        item.getQuantity(),
                        TYPE_DEDUCT,
                        "Async deduct stock for order #" + order.getId()
                );
            }
        }

        setStatus(order, STATUS_CONFIRMED);
        createNotification(
                order.getUserId(),
                "Đơn hàng đã được xác nhận",
                "Đơn #" + order.getId() + " đã xử lý thành công.",
                "ORDER_CONFIRMED"
        );
        if (userEmail != null && !userEmail.isBlank()) {
            emailService.sendOrderConfirmationEmail(userEmail, order.getId());
        }
    }

    private void setStatus(OrderEntity order, String status) {
        order.setStatus(status);
        orderRepository.save(order);

        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setOrderId(order.getId());
        history.setStatus(status);
        history.setChangedAt(LocalDateTime.now());
        orderStatusHistoryRepository.save(history);
    }

    private void createNotification(Long userId, String title, String content, String type) {
        NotificationEntity notification = new NotificationEntity();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }
}
