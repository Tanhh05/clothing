package com.clothing.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ToggleWishlistItemRequest {

    @NotNull
    private Long productId;
}
