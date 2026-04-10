package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ReviewResponse {

    private Long id;
    private Long orderId;
    private Long productId;
    private Long userId;
    private String username;
    private Integer rating;
    private String comment;
    private String size;
    private String color;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
}
