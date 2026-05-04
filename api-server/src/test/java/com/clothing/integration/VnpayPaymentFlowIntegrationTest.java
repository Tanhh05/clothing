package com.clothing.integration;

import com.clothing.entity.MomoCheckoutSessionEntity;
import com.clothing.entity.PaymentEntity;
import com.clothing.entity.ProductVariantEntity;
import com.clothing.entity.UserEntity;
import com.clothing.entity.VnpayCheckoutSessionEntity;
import com.clothing.job.CheckoutSessionCleanupJob;
import com.clothing.repository.MomoCheckoutSessionRepository;
import com.clothing.repository.OrderRepository;
import com.clothing.repository.PaymentRepository;
import com.clothing.repository.ProductVariantRepository;
import com.clothing.repository.UserRepository;
import com.clothing.repository.VnpayCheckoutSessionRepository;
import com.clothing.service.AuditLogService;
import com.clothing.service.GhnShippingService;
import com.clothing.service.InventoryMovementService;
import com.clothing.service.OrderService;
import com.clothing.service.PaymentService;
import com.clothing.service.StoreSettingService;
import com.clothing.messaging.publisher.OrderEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class VnpayPaymentFlowIntegrationTest {

    private static final String VNPAY_SECRET = "SECRETKEY";

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CheckoutSessionCleanupJob checkoutSessionCleanupJob;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private VnpayCheckoutSessionRepository vnpayCheckoutSessionRepository;

    @Autowired
    private MomoCheckoutSessionRepository momoCheckoutSessionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GhnShippingService ghnShippingService;

    @MockBean
    private OrderEventPublisher orderEventPublisher;

    @MockBean
    private AuditLogService auditLogService;

    @MockBean
    private StoreSettingService storeSettingService;

    @MockBean
    private InventoryMovementService inventoryMovementService;

    @BeforeEach
    void setup() {
        when(ghnShippingService.canCallShippingApi()).thenReturn(true);
        when(ghnShippingService.createShippingOrder(any())).thenReturn("GHN_TEST_CODE");
    }

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
        paymentRepository.deleteAll();
        vnpayCheckoutSessionRepository.deleteAll();
        momoCheckoutSessionRepository.deleteAll();
        productVariantRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void validSignatureCreatesOrderSuccessfully() {
        TestData testData = seedVnpaySession(false);
        Map<String, String> payload = buildSignedVnpayIpnPayload(testData.txnRef, false);

        paymentService.handleVnpayIpn(payload);
        orderService.handleVnpayPaymentIpn(payload);

        PaymentEntity payment = paymentRepository.findByTransactionCode(testData.txnRef).orElseThrow();
        VnpayCheckoutSessionEntity session = vnpayCheckoutSessionRepository.findAll().stream()
                .filter(item -> testData.session.getToken().equals(item.getToken()))
                .findFirst()
                .orElseThrow();
        List<?> orders = orderRepository.findByUserIdOrderByIdDesc(testData.user.getId());

        assertEquals("PAID", payment.getStatus());
        assertEquals("COMPLETED", session.getStatus());
        assertNotNull(session.getCreatedOrderId());
        assertEquals(1, orders.size());
    }

    @Test
    void invalidSignatureIsRejected() {
        TestData testData = seedVnpaySession(false);
        Map<String, String> payload = buildSignedVnpayIpnPayload(testData.txnRef, true);

        assertThrows(RuntimeException.class, () -> paymentService.handleVnpayIpn(payload));

        PaymentEntity payment = paymentRepository.findByTransactionCode(testData.txnRef).orElseThrow();
        VnpayCheckoutSessionEntity session = vnpayCheckoutSessionRepository.findAll().stream()
                .filter(item -> testData.session.getToken().equals(item.getToken()))
                .findFirst()
                .orElseThrow();
        List<?> orders = orderRepository.findByUserIdOrderByIdDesc(testData.user.getId());

        assertEquals("CREATED", payment.getStatus());
        assertEquals("PENDING", session.getStatus());
        assertEquals(0, orders.size());
    }

    @Test
    void duplicateIpnDoesNotCreateDuplicateOrder() {
        TestData testData = seedVnpaySession(false);
        Map<String, String> payload = buildSignedVnpayIpnPayload(testData.txnRef, false);

        paymentService.handleVnpayIpn(payload);
        orderService.handleVnpayPaymentIpn(payload);

        paymentService.handleVnpayIpn(payload);
        orderService.handleVnpayPaymentIpn(payload);

        VnpayCheckoutSessionEntity session = vnpayCheckoutSessionRepository.findAll().stream()
                .filter(item -> testData.session.getToken().equals(item.getToken()))
                .findFirst()
                .orElseThrow();
        List<?> orders = orderRepository.findByUserIdOrderByIdDesc(testData.user.getId());

        assertEquals("COMPLETED", session.getStatus());
        assertNotNull(session.getCreatedOrderId());
        assertEquals(1, orders.size());
    }

    @Test
    void expiredSessionDoesNotCreateOrder() {
        TestData testData = seedVnpaySession(true);
        Map<String, String> payload = buildSignedVnpayIpnPayload(testData.txnRef, false);

        paymentService.handleVnpayIpn(payload);
        orderService.handleVnpayPaymentIpn(payload);

        VnpayCheckoutSessionEntity session = vnpayCheckoutSessionRepository.findAll().stream()
                .filter(item -> testData.session.getToken().equals(item.getToken()))
                .findFirst()
                .orElseThrow();
        List<?> orders = orderRepository.findByUserIdOrderByIdDesc(testData.user.getId());

        assertEquals("EXPIRED", session.getStatus());
        assertNull(session.getCreatedOrderId());
        assertEquals(0, orders.size());
    }

    @Test
    void cleanupJobDeletesOnlyExpiredPendingSessions() {
        MomoCheckoutSessionEntity expiredMomo = new MomoCheckoutSessionEntity();
        expiredMomo.setToken("MOMO_EXPIRED");
        expiredMomo.setUserId(1L);
        expiredMomo.setRequestPayload("{}");
        expiredMomo.setCartItemsPayload("[]");
        expiredMomo.setSubTotal(1L);
        expiredMomo.setShippingFee(0L);
        expiredMomo.setDiscountAmount(0L);
        expiredMomo.setTotalPrice(1L);
        expiredMomo.setStatus("PENDING");
        expiredMomo.setCreatedAt(LocalDateTime.now().minusHours(1));
        expiredMomo.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        momoCheckoutSessionRepository.save(expiredMomo);

        MomoCheckoutSessionEntity activeMomo = new MomoCheckoutSessionEntity();
        activeMomo.setToken("MOMO_ACTIVE");
        activeMomo.setUserId(1L);
        activeMomo.setRequestPayload("{}");
        activeMomo.setCartItemsPayload("[]");
        activeMomo.setSubTotal(1L);
        activeMomo.setShippingFee(0L);
        activeMomo.setDiscountAmount(0L);
        activeMomo.setTotalPrice(1L);
        activeMomo.setStatus("PENDING");
        activeMomo.setCreatedAt(LocalDateTime.now());
        activeMomo.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        momoCheckoutSessionRepository.save(activeMomo);

        VnpayCheckoutSessionEntity expiredVnpay = new VnpayCheckoutSessionEntity();
        expiredVnpay.setToken("VNPAY_EXPIRED");
        expiredVnpay.setUserId(1L);
        expiredVnpay.setRequestPayload("{}");
        expiredVnpay.setCartItemsPayload("[]");
        expiredVnpay.setSubTotal(1L);
        expiredVnpay.setShippingFee(0L);
        expiredVnpay.setDiscountAmount(0L);
        expiredVnpay.setTotalPrice(1L);
        expiredVnpay.setStatus("PENDING");
        expiredVnpay.setCreatedAt(LocalDateTime.now().minusHours(1));
        expiredVnpay.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        vnpayCheckoutSessionRepository.save(expiredVnpay);

        checkoutSessionCleanupJob.cleanupExpiredPendingSessions();

        assertTrue(momoCheckoutSessionRepository.findAll().stream().noneMatch(session -> "MOMO_EXPIRED".equals(session.getToken())));
        assertTrue(vnpayCheckoutSessionRepository.findAll().stream().noneMatch(session -> "VNPAY_EXPIRED".equals(session.getToken())));
        assertTrue(momoCheckoutSessionRepository.findAll().stream().anyMatch(session -> "MOMO_ACTIVE".equals(session.getToken())));
    }

    private TestData seedVnpaySession(boolean expiredSession) {
        UserEntity user = new UserEntity();
        user.setUsername("user_" + System.nanoTime());
        user.setEmail("user_" + System.nanoTime() + "@example.com");
        user.setPassword("encoded-password");
        user.setFullName("Test User");
        user.setPhone("0901234567");
        user.setStatus("ACTIVE");
        user = userRepository.save(user);

        ProductVariantEntity variant = new ProductVariantEntity();
        variant.setSku("SKU_" + System.nanoTime());
        variant.setPrice(250_000L);
        variant.setStock(10);
        variant.setWeight(0.2D);
        variant.setStatus("ACTIVE");
        variant = productVariantRepository.save(variant);

        String token = ("token" + System.nanoTime()).replace("-", "");
        String txnRef = "TMP" + token + "-" + System.currentTimeMillis();

        Map<String, Object> requestSnapshot = new LinkedHashMap<>();
        requestSnapshot.put("paymentMethod", "VNPAY");
        requestSnapshot.put("address", "123 Test");
        requestSnapshot.put("recipientName", "Tester");
        requestSnapshot.put("phone", "0901234567");
        requestSnapshot.put("province", "Ha Noi");
        requestSnapshot.put("district", "Ba Dinh");
        requestSnapshot.put("ward", "Phuc Xa");

        Map<String, Object> itemSnapshot = new LinkedHashMap<>();
        itemSnapshot.put("variantId", variant.getId());
        itemSnapshot.put("quantity", 1);
        itemSnapshot.put("unitPrice", 250_000L);

        VnpayCheckoutSessionEntity session = new VnpayCheckoutSessionEntity();
        session.setToken(token);
        session.setUserId(user.getId());
        session.setRequestPayload(writeJson(requestSnapshot));
        session.setCartItemsPayload(writeJson(List.of(itemSnapshot)));
        session.setSubTotal(250_000L);
        session.setShippingFee(0L);
        session.setDiscountAmount(0L);
        session.setTotalPrice(250_000L);
        session.setStatus("PENDING");
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(expiredSession ? LocalDateTime.now().minusMinutes(1) : LocalDateTime.now().plusMinutes(15));
        session = vnpayCheckoutSessionRepository.save(session);

        PaymentEntity payment = new PaymentEntity();
        payment.setAmount(250_000L);
        payment.setMethod("VNPAY");
        payment.setStatus("CREATED");
        payment.setTransactionCode(txnRef);
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return new TestData(user, session, txnRef);
    }

    private Map<String, String> buildSignedVnpayIpnPayload(String txnRef, boolean invalidSignature) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("vnp_Amount", "25000000");
        payload.put("vnp_BankCode", "NCB");
        payload.put("vnp_BankTranNo", "TESTBANK123456");
        payload.put("vnp_CardType", "ATM");
        payload.put("vnp_OrderInfo", "Thanh toan don hang tam");
        payload.put("vnp_PayDate", "20260421203000");
        payload.put("vnp_ResponseCode", "00");
        payload.put("vnp_TmnCode", "2QXUI4J4");
        payload.put("vnp_TransactionNo", "99999999");
        payload.put("vnp_TransactionStatus", "00");
        payload.put("vnp_TxnRef", txnRef);

        String signature = sign(buildSigningData(payload), VNPAY_SECRET);
        payload.put("vnp_SecureHash", invalidSignature ? signature + "aa" : signature);
        return payload;
    }

    private String buildSigningData(Map<String, String> payload) {
        TreeMap<String, String> sorted = new TreeMap<>(payload);
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if ("vnp_SecureHash".equalsIgnoreCase(entry.getKey()) || "vnp_SecureHashType".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            if (!first) {
                builder.append('&');
            }
            builder.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
            first = false;
        }
        return builder.toString();
    }

    private String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private record TestData(UserEntity user, VnpayCheckoutSessionEntity session, String txnRef) {}
}
