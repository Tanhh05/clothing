package com.clothing.repository;

import com.clothing.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    List<OrderItemEntity> findByOrderIdOrderByIdAsc(Long orderId);

    List<OrderItemEntity> findByOrderIdInOrderByOrderIdAscIdAsc(List<Long> orderIds);

    List<OrderItemEntity> findByIdIn(Set<Long> ids);

    @Query(value = """
            select
                pv.product_id as productId,
                p.name as productName,
                coalesce(sum(coalesce(oi.quantity, 0)), 0) as totalQuantity,
                coalesce(sum(coalesce(oi.price, 0) * coalesce(oi.quantity, 0)), 0) as totalRevenue
            from order_items oi
            join orders o on o.id = oi.order_id
            join product_variants pv on pv.id = oi.variant_id
            join products p on p.id = pv.product_id
            where o.created_at >= date_trunc('day', now()) - interval '29 day'
              and upper(coalesce(o.status, '')) = 'DELIVERED'
            group by pv.product_id, p.name
            order by totalQuantity desc
            limit 5
            """, nativeQuery = true)
    List<TopProductSalesProjection> findTopProductSales30dDelivered();
}
