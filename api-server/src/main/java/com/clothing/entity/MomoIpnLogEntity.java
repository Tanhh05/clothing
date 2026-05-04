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
@Table(name = "momo_ipn_logs")
public class MomoIpnLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", length = 120)
    private String orderId;

    @Column(name = "request_id", length = 120)
    private String requestId;

    @Column(name = "trans_id", length = 120)
    private String transId;

    @Column(name = "result_code")
    private Integer resultCode;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "process_status", length = 20)
    private String processStatus;

    @Column(name = "process_message", length = 500)
    private String processMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
