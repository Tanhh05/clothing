package com.clothing.service;

import com.clothing.dto.response.WishlistResponse;

public interface WishlistService {

    WishlistResponse getMyWishlist(String username);

    WishlistResponse addItem(String username, Long productId);

    WishlistResponse removeItem(String username, Long productId);
}
