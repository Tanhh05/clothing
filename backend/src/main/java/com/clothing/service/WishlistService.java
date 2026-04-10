package com.clothing.service;

import com.clothing.dto.response.WishlistResponse;
import com.clothing.dto.response.WishlistDealResponse;

import java.util.List;

public interface WishlistService {

    WishlistResponse getMyWishlist(String username);

    WishlistResponse addItem(String username, Long productId);

    WishlistResponse removeItem(String username, Long productId);

    void upsertPriceAlert(String username, Long productId, Long targetPrice);

    List<WishlistDealResponse> getDeals(String username);
}
