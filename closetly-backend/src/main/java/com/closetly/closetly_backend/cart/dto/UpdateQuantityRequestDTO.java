package com.closetly.closetly_backend.cart.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateQuantityRequestDTO {
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}