package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WishlistDealResponse {

    private Long productId;
    private String productName;
    private String productSlug;
    private Long currentMinPrice;
    private Long targetPrice;
    private Long diff;
}
