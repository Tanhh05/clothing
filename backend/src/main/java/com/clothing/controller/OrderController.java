package com.clothing.controller;

import com.clothing.dto.request.CreateOrderRequest;
import com.clothing.dto.request.OrderBulkStatusRequest;
import com.clothing.dto.request.UpdateOrderStatusRequest;
import com.clothing.dto.response.AdminDashboardSummaryResponse;
import com.clothing.dto.response.OrderResponse;
import com.clothing.dto.response.PageResponse;
import com.clothing.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return ResponseEntity.ok(orderService.createOrder(authentication.getName(), request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<OrderResponse>> getAdminOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(orderService.getAdminOrders(page, size, sortBy, direction, q, status, fromDate, toDate));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardSummaryResponse> getAdminSummary() {
        return ResponseEntity.ok(orderService.getAdminDashboardSummary());
    }

    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication authentication) {
        return ResponseEntity.ok(orderService.getMyOrders(authentication.getName()));
    }

    @GetMapping("/my/{orderId}")
    public ResponseEntity<OrderResponse> getMyOrderById(Authentication authentication, @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getMyOrderById(authentication.getName(), orderId));
    }

    @PostMapping("/my/{orderId}/reorder")
    public ResponseEntity<OrderResponse> reorder(Authentication authentication, @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.reorder(authentication.getName(), orderId));
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request));
    }

    @PatchMapping("/bulk/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> bulkUpdateStatus(@Valid @RequestBody OrderBulkStatusRequest request) {
        int affected = orderService.bulkUpdateStatus(request.getIds(), request.getStatus());
        return ResponseEntity.ok(Map.of("affected", affected));
    }

    @PostMapping("/{orderId}/status/sync-ghn")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> syncOrderStatusWithGhn(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.syncOrderStatusWithGhn(orderId));
    }
}
