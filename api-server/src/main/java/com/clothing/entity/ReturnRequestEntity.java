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
@Table(name = "return_requests")
public class ReturnRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "return_type", length = 20)
    private String returnType;

    @Column(name = "reason_code", length = 50)
    private String reasonCode;

    @Column(name = "reason_detail", columnDefinition = "TEXT")
    private String reasonDetail;

    @Column(name = "evidence_urls", columnDefinition = "TEXT")
    private String evidenceUrls;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(length = 20)
    private String status;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;
}
