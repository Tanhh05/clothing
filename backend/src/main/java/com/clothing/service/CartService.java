package com.clothing.service;

import com.clothing.dto.request.AddCartItemRequest;
import com.clothing.dto.request.UpdateCartItemRequest;
import com.clothing.dto.response.CartResponse;

public interface CartService {

    CartResponse getMyCart(String username);

    CartResponse addItem(String username, AddCartItemRequest request);

    CartResponse updateItem(String username, Long cartItemId, UpdateCartItemRequest request);

    CartResponse removeItem(String username, Long cartItemId);
}
