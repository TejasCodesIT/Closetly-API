package com.closetly.closetly_backend.wishlist.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddToWishlistRequestDTO {
    @NotNull(message = "Product ID is required")
    private Long productId;
}