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
@Table(name = "return_request_items")
public class ReturnRequestItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_request_id")
    private Long returnRequestId;

    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "sku", length = 120)
    private String sku;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "requested_quantity")
    private Integer requestedQuantity;
}
