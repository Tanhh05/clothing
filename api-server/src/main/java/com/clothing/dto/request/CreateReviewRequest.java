package com.clothing.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateReviewRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;

    @NotNull(message = "productId is required")
    private Long productId;

    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be >= 1")
    @Max(value = 5, message = "rating must be <= 5")
    private Integer rating;

    @Size(max = 1000, message = "comment max length is 1000")
    private String comment;

    @Size(max = 5, message = "imageUrls max length is 5")
    private List<@Size(max = 500, message = "image url max length is 500") String> imageUrls;
}
