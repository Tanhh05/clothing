package com.clothing.repository;

import com.clothing.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long>, JpaSpecificationExecutor<OrderEntity> {

    List<OrderEntity> findByUserIdOrderByIdDesc(Long userId);

    List<OrderEntity> findAllByOrderByIdDesc();

    Optional<OrderEntity> findByShippingCode(String shippingCode);

    @Query("select distinct o.userId from OrderEntity o where o.userId is not null")
    List<Long> findDistinctUserIds();

    @Query("""
            select distinct upper(trim(o.status))
            from OrderEntity o
            where o.status is not null and trim(o.status) <> ''
            order by upper(trim(o.status))
            """)
    List<String> findDistinctOrderStatuses();

    @Query("""
            select distinct lower(trim(o.shippingStatus))
            from OrderEntity o
            where o.shippingStatus is not null and trim(o.shippingStatus) <> ''
            order by lower(trim(o.shippingStatus))
            """)
    List<String> findDistinctShippingStatuses();

    @Query(value = """
            select
                coalesce(sum(case
                    when o.created_at >= date_trunc('day', now())
                         and upper(coalesce(o.status, '')) = 'DELIVERED'
                    then coalesce(o.total_price, 0) else 0 end), 0) as "revenueToday",
                coalesce(sum(case
                    when o.created_at >= date_trunc('day', now()) - interval '6 day'
                         and upper(coalesce(o.status, '')) = 'DELIVERED'
                    then coalesce(o.total_price, 0) else 0 end), 0) as "revenue7d",
                coalesce(sum(case
                    when o.created_at >= date_trunc('day', now()) - interval '29 day'
                         and upper(coalesce(o.status, '')) = 'DELIVERED'
                    then coalesce(o.total_price, 0) else 0 end), 0) as "revenue30d",
                count(case when o.created_at >= date_trunc('day', now()) then 1 end) as "ordersToday",
                count(case when o.created_at >= date_trunc('day', now()) - interval '6 day' then 1 end) as "orders7d",
                count(case when o.created_at >= date_trunc('day', now()) - interval '29 day' then 1 end) as "orders30d",
                count(case when upper(coalesce(o.status, '')) in ('PENDING', 'PROCESSING', 'CONFIRMED') then 1 end) as "pendingOrders",
                count(case
                    when o.created_at >= date_trunc('day', now()) - interval '29 day'
                         and upper(coalesce(o.status, '')) in ('CANCELLED', 'FAILED', 'FAILED_DELIVERY', 'FAILED_INSUFFICIENT_STOCK', 'REFUNDED')
                    then 1 end) as "cancelLike30d",
                count(case when o.created_at >= date_trunc('day', now()) - interval '29 day' then 1 end) as "total30d"
            from orders o
            """, nativeQuery = true)
    DashboardMetricsProjection fetchDashboardMetrics();

    @Query(value = """
            select upper(coalesce(o.status, '')) as status, count(*) as total
            from orders o
            where o.created_at >= date_trunc('day', now()) - interval '29 day'
            group by upper(coalesce(o.status, ''))
            """, nativeQuery = true)
    List<StatusCountProjection> findStatusCounts30d();
}
