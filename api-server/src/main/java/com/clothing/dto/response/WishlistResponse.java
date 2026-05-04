package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class WishlistResponse {

    private Long wishlistId;
    private Long userId;
    private List<Long> productIds;
    private Map<Long, Long> priceAlertTargets;
}
