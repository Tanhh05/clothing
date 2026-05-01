package com.clothing.integration;

import com.clothing.dto.request.UpdateOrderStatusRequest;
import com.clothing.entity.OrderEntity;
import com.clothing.entity.OrderItemEntity;
import com.clothing.entity.ProductVariantEntity;
import com.clothing.entity.StockReservationEntity;
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
class CodReservationIntegrationTest {

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
    void codReservationIsCapturedWhenOrderMovesToShipped() {
        UserEntity user = new UserEntity();
        user.setUsername("cod_capture_user_" + System.nanoTime());
        user.setEmail("cod_capture_user_" + System.nanoTime() + "@example.com");
        user.setPassword("encoded-password");
        user.setStatus("ACTIVE");
        user = userRepository.save(user);

        ProductVariantEntity variant = new ProductVariantEntity();
        variant.setSku("SKU_COD_CAPTURE_" + System.nanoTime());
        variant.setPrice(200_000L);
        variant.setStock(10);
        variant.setStatus("ACTIVE");
        variant = productVariantRepository.save(variant);

        OrderEntity order = new OrderEntity();
        order.setUserId(user.getId());
        order.setSubTotal(400_000L);
        order.setTotalPrice(400_000L);
        order.setShippingFee(0L);
        order.setDiscountAmount(0L);
        order.setPaymentMethod("COD");
        order.setStatus("CONFIRMED");
        order.setShippingProvider("GHN");
        order.setAddress("Test COD address");
        order.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        order = orderRepository.save(order);

        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setOrderId(order.getId());
        orderItem.setVariantId(variant.getId());
        orderItem.setQuantity(2);
        orderItem.setPrice(200_000L);
        orderItemRepository.save(orderItem);

        StockReservationEntity reservation = new StockReservationEntity();
        reservation.setOrderId(order.getId());
        reservation.setVariantId(variant.getId());
        reservation.setQuantity(2);
        reservation.setStatus("ACTIVE");
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(60));
        reservation.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        stockReservationRepository.save(reservation);

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus("SHIPPED");
        request.setSyncWithGhn(false);
        orderService.updateOrderStatus(order.getId(), request);

        ProductVariantEntity updatedVariant = productVariantRepository.findById(variant.getId()).orElseThrow();
        boolean hasConvertedReservation = stockReservationRepository
                .findByOrderIdAndStatusIgnoreCaseOrderByIdAsc(order.getId(), "CONVERTED")
                .size() == 1;

        assertEquals(8, updatedVariant.getStock());
        assertTrue(hasConvertedReservation);
    }

    @Test
    void expiredCodReservedOrderIsCancelledAndReservationReleased() {
        UserEntity user = new UserEntity();
        user.setUsername("cod_timeout_user_" + System.nanoTime());
        user.setEmail("cod_timeout_user_" + System.nanoTime() + "@example.com");
        user.setPassword("encoded-password");
        user.setStatus("ACTIVE");
        user = userRepository.save(user);

        ProductVariantEntity variant = new ProductVariantEntity();
        variant.setSku("SKU_COD_TIMEOUT_" + System.nanoTime());
        variant.setPrice(180_000L);
        variant.setStock(9);
        variant.setStatus("ACTIVE");
        variant = productVariantRepository.save(variant);

        OrderEntity order = new OrderEntity();
        order.setUserId(user.getId());
        order.setSubTotal(180_000L);
        order.setTotalPrice(180_000L);
        order.setShippingFee(0L);
        order.setDiscountAmount(0L);
        order.setPaymentMethod("COD");
        order.setStatus("CONFIRMED");
        order.setShippingProvider("GHN");
        order.setAddress("Test COD timeout address");
        order.setCreatedAt(LocalDateTime.now().minusHours(4));
        order = orderRepository.save(order);

        StockReservationEntity reservation = new StockReservationEntity();
        reservation.setOrderId(order.getId());
        reservation.setVariantId(variant.getId());
        reservation.setQuantity(1);
        reservation.setStatus("ACTIVE");
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        reservation.setCreatedAt(LocalDateTime.now().minusHours(4));
        stockReservationRepository.save(reservation);

        int cancelled = orderService.cancelExpiredCodReservedOrders();

        OrderEntity updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        ProductVariantEntity updatedVariant = productVariantRepository.findById(variant.getId()).orElseThrow();
        boolean hasReleasedReservation = stockReservationRepository
                .findByOrderIdAndStatusIgnoreCaseOrderByIdAsc(order.getId(), "RELEASED")
                .size() == 1;

        assertEquals(1, cancelled);
        assertEquals("CANCELLED", updatedOrder.getStatus());
        assertEquals(9, updatedVariant.getStock());
        assertTrue(hasReleasedReservation);
    }
}
