package com.clothing.controller;

import com.clothing.dto.request.ToggleWishlistItemRequest;
import com.clothing.dto.request.WishlistPriceAlertRequest;
import com.clothing.dto.response.WishlistDealResponse;
import com.clothing.dto.response.WishlistResponse;
import com.clothing.service.WishlistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ResponseEntity<WishlistResponse> getMyWishlist(Authentication authentication) {
        return ResponseEntity.ok(wishlistService.getMyWishlist(authentication.getName()));
    }

    @PostMapping("/items")
    public ResponseEntity<WishlistResponse> addItem(
            Authentication authentication,
            @Valid @RequestBody ToggleWishlistItemRequest request
    ) {
        return ResponseEntity.ok(wishlistService.addItem(authentication.getName(), request.getProductId()));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<WishlistResponse> removeItem(
            Authentication authentication,
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(wishlistService.removeItem(authentication.getName(), productId));
    }

    @PostMapping("/price-alerts")
    public ResponseEntity<Void> upsertPriceAlert(
            Authentication authentication,
            @Valid @RequestBody WishlistPriceAlertRequest request
    ) {
        wishlistService.upsertPriceAlert(authentication.getName(), request.getProductId(), request.getTargetPrice());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/deals")
    public ResponseEntity<List<WishlistDealResponse>> getDeals(Authentication authentication) {
        return ResponseEntity.ok(wishlistService.getDeals(authentication.getName()));
    }
}
