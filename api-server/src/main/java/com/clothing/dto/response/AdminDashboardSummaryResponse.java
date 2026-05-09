package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class AdminDashboardSummaryResponse {

    private Long revenueToday;
    private Long revenue7d;
    private Long revenue30d;
    private Long ordersToday;
    private Long orders7d;
    private Long orders30d;
    private Long pendingOrders;
    private Double cancelRate30d;
    private Map<String, Long> statusCounts30d;
    private List<ProductSalesStatResponse> topProducts30d;
    private List<TopBuyerStatResponse> topBuyers30d;
}
