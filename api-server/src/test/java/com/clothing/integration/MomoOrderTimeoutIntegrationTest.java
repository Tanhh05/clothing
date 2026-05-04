package com.clothing.integration;

import com.clothing.entity.OrderEntity;
import com.clothing.entity.OrderItemEntity;
import com.clothing.entity.OrderStatusHistoryEntity;
import com.clothing.entity.PaymentEntity;
import com.clothing.entity.ProductVariantEntity;
import com.clothing.entity.UserEntity;
import com.clothing.messaging.publisher.OrderEventPublisher;
import com.clothing.repository.InventoryLogRepository;
import com.clothing.repository.OrderItemRepository;
import com.clothing.repository.OrderRepository;
import com.clothing.repository.OrderStatusHistoryRepository;
import com.clothing.repository.PaymentRepository;
import com.clothing.repository.ProductVariantRepository;
import com.clothing.repository.StockReservationRepository;
import com.clothing.repository.UserRepository;
import com.clothing.service.AuditLogService;
import com.clothing.service.GhnShippingService;
import com.clothing.service.OrderService;
import com.clothing.service.StoreSettingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class MomoOrderTimeoutIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private InventoryLogRepository inventoryLogRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @MockBean
    private GhnShippingService ghnShippingService;

    @MockBean
    private OrderEventPublisher orderEventPublisher;

    @MockBean
    private AuditLogService auditLogService;

    @MockBean
    private StoreSettingService storeSettingService;

    @AfterEach
    void cleanup() {
        orderStatusHistoryRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        paymentRepository.deleteAll();
        inventoryLogRepository.deleteAll();
        stockReservationRepository.deleteAll();
        productVariantRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void expiredWaitingPaymentMomoPosOrderIsCancelledAndStockRestored() {
        UserEntity user = new UserEntity();
        user.setUsername("timeout_user_" + System.nanoTime());
        user.setEmail("timeout_user_" + System.nanoTime() + "@example.com");
        user.setPassword("encoded-password");
        user.setStatus("ACTIVE");
        user = userRepository.save(user);

        ProductVariantEntity variant = new ProductVariantEntity();
        variant.setSku("SKU_TIMEOUT_" + System.nanoTime());
        variant.setPrice(150_000L);
        variant.setStock(3);
        variant.setStatus("ACTIVE");
        variant = productVariantRepository.save(variant);

        OrderEntity order = new OrderEntity();
        order.setUserId(user.getId());
        order.setSubTotal(300_000L);
        order.setTotalPrice(300_000L);
        order.setShippingFee(0L);
        order.setDiscountAmount(0L);
        order.setPaymentMethod("MOMO");
        order.setStatus("WAITING_PAYMENT");
        order.setShippingProvider("POS_COUNTER");
        order.setAddress("BAN_TAI_QUAY | Khach: Test");
        order.setCreatedAt(LocalDateTime.now().minusMinutes(40));
        order = orderRepository.save(order);

        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setOrderId(order.getId());
        orderItem.setVariantId(variant.getId());
        orderItem.setQuantity(2);
        orderItem.setPrice(150_000L);
        orderItemRepository.save(orderItem);

        com.clothing.entity.StockReservationEntity reservation = new com.clothing.entity.StockReservationEntity();
        reservation.setOrderId(order.getId());
        reservation.setVariantId(variant.getId());
        reservation.setQuantity(2);
        reservation.setStatus("ACTIVE");
        reservation.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        reservation.setCreatedAt(LocalDateTime.now().minusMinutes(40));
        stockReservationRepository.save(reservation);

        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(order.getId());
        payment.setMethod("MOMO");
        payment.setAmount(300_000L);
        payment.setStatus("CREATED");
        payment.setTransactionCode("ORD" + order.getId() + "-" + System.nanoTime());
        payment.setCreatedAt(LocalDateTime.now().minusMinutes(40));
        paymentRepository.save(payment);

        int cancelled = orderService.cancelExpiredMomoWaitingPaymentOrders();

        OrderEntity updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        ProductVariantEntity updatedVariant = productVariantRepository.findById(variant.getId()).orElseThrow();
        PaymentEntity updatedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        boolean hasReleasedReservation = stockReservationRepository
                .findByOrderIdAndStatusIgnoreCaseOrderByIdAsc(order.getId(), "RELEASED")
                .size() == 1;
        boolean hasCancelledHistory = orderStatusHistoryRepository.findByOrderIdOrderByIdAsc(order.getId()).stream()
                .map(OrderStatusHistoryEntity::getStatus)
                .anyMatch(status -> "CANCELLED".equalsIgnoreCase(status));

        assertEquals(1, cancelled);
        assertEquals("CANCELLED", updatedOrder.getStatus());
        assertEquals(3, updatedVariant.getStock());
        assertEquals("FAILED", updatedPayment.getStatus());
        assertTrue(hasReleasedReservation);
        assertTrue(hasCancelledHistory);
    }
}
