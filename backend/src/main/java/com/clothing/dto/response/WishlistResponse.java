package com.clothing.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WishlistResponse {

    private Long wishlistId;
    private Long userId;
    private List<Long> productIds;
}
