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
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "total_price")
    private Long totalPrice;

    @Column(name = "sub_total")
    private Long subTotal;

    @Column(name = "shipping_fee")
    private Long shippingFee;

    @Column(name = "discount_amount")
    private Long discountAmount;

    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(length = 50)
    private String status;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "shipping_provider", length = 50)
    private String shippingProvider;

    @Column(name = "shipping_code", length = 100, unique = true)
    private String shippingCode;

    @Column(name = "shipping_status", length = 50)
    private String shippingStatus;

    @Column(name = "shipping_updated_at")
    private LocalDateTime shippingUpdatedAt;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
