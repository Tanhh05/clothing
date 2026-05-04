package com.clothing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "warehouse_inbound_items")
public class WarehouseInboundItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inbound_id")
    private Long inboundId;

    @Column(length = 100)
    private String sku;

    private Integer quantity;

    @Column(name = "unit_cost")
    private Long unitCost;

    @Column(name = "line_total")
    private Long lineTotal;
}
