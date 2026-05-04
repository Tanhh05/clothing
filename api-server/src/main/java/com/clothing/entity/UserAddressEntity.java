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
@Table(name = "user_addresses")
public class UserAddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "recipient_name", length = 120)
    private String recipientName;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "province", length = 120)
    private String province;

    @Column(name = "district", length = 120)
    private String district;

    @Column(name = "ward", length = 120)
    private String ward;

    @Column(name = "address_line", columnDefinition = "TEXT")
    private String addressLine;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
