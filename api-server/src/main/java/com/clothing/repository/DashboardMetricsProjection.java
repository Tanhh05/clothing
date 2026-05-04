package com.clothing.repository;

public interface DashboardMetricsProjection {

    Long getRevenueToday();

    Long getRevenue7d();

    Long getRevenue30d();

    Long getOrdersToday();

    Long getOrders7d();

    Long getOrders30d();

    Long getPendingOrders();

    Long getCancelLike30d();

    Long getTotal30d();
}
