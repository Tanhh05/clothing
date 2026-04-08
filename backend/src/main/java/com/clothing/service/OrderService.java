package com.clothing.service;

import com.clothing.dto.request.CreateOrderRequest;
import com.clothing.dto.request.UpdateOrderStatusRequest;
import com.clothing.dto.response.AdminDashboardSummaryResponse;
import com.clothing.dto.response.OrderResponse;
import com.clothing.dto.response.PageResponse;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {

    OrderResponse createOrder(String username, CreateOrderRequest request);

    List<OrderResponse> getAllOrders();

    AdminDashboardSummaryResponse getAdminDashboardSummary();

    PageResponse<OrderResponse> getAdminOrders(
            int page,
            int size,
            String sortBy,
            String direction,
            String q,
            String status,
            LocalDate fromDate,
            LocalDate toDate
    );

    List<OrderResponse> getMyOrders(String username);

    OrderResponse getMyOrderById(String username, Long orderId);

    OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request);

    int bulkUpdateStatus(List<Long> ids, String status);
}
