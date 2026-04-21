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
@Table(name = "momo_checkout_sessions")
public class MomoCheckoutSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 120, nullable = false, unique = true)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "request_payload", columnDefinition = "TEXT", nullable = false)
    private String requestPayload;

    @Column(name = "cart_items_payload", columnDefinition = "TEXT", nullable = false)
    private String cartItemsPayload;

    @Column(name = "sub_total", nullable = false)
    private Long subTotal;

    @Column(name = "shipping_fee", nullable = false)
    private Long shippingFee;

    @Column(name = "discount_amount", nullable = false)
    private Long discountAmount;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(length = 30, nullable = false)
    private String status;

    @Column(name = "payment_transaction_code", length = 120)
    private String paymentTransactionCode;

    @Column(name = "created_order_id")
    private Long createdOrderId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
