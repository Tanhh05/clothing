package com.clothing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "warehouse_inbounds")
public class WarehouseInboundEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, unique = true)
    private String code;

    @Column(length = 255)
    private String supplier;

    @Column(name = "inbound_at")
    private LocalDateTime inboundAt;

    @Column(name = "item_count")
    private Integer itemCount;

    @Column(name = "total_cost")
    private Long totalCost;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
