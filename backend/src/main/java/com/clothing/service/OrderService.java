package com.clothing.service;

import com.clothing.dto.request.CreateOrderRequest;
import com.clothing.dto.request.PosCheckoutRequest;
import com.clothing.dto.request.UpdateOrderStatusRequest;
import com.clothing.dto.response.AdminDashboardSummaryResponse;
import com.clothing.dto.response.OrderResponse;
import com.clothing.dto.response.PageResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface OrderService {

    OrderResponse createOrder(String username, CreateOrderRequest request);

    OrderResponse createPosOrder(String adminUsername, PosCheckoutRequest request);

    List<OrderResponse> getAllOrders();

    AdminDashboardSummaryResponse getAdminDashboardSummary();

    PageResponse<OrderResponse> getAdminOrders(
            int page,
            int size,
            String sortBy,
            String direction,
            String q,
            String status,
            String shippingStatus,
            LocalDate fromDate,
            LocalDate toDate
    );

    List<OrderResponse> getMyOrders(String username);
    PageResponse<OrderResponse> getMyOrders(String username, int page, int size);

    OrderResponse getMyOrderById(String username, Long orderId);

    OrderResponse reorder(String username, Long orderId);

    OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request);

    int bulkUpdateStatus(List<Long> ids, String status);

    List<OrderResponse> getAdminInvoices(List<Long> ids);

    byte[] exportAdminInvoicesExcel(List<Long> ids);

    OrderResponse syncOrderStatusWithGhn(Long orderId);

    Map<String, List<String>> getAdminStatusOptions();

    void handleGhnWebhook(Map<String, Object> payload);

    void handleMomoPaymentIpn(Map<String, Object> payload);

    void handleVnpayPaymentIpn(Map<String, String> payload);

    int cancelExpiredMomoWaitingPaymentOrders();

    int cancelExpiredCodReservedOrders();

    void sendMyOrderConfirmationEmail(String username, Long orderId);
}
