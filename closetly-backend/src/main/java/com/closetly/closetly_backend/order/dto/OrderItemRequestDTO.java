package com.closetly.closetly_backend.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class OrderItemRequestDTO {
    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Order type is required")
    private String type; // BUY or RENT

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity = 1;

    // For rentals
    private LocalDate startDate;
    private LocalDate endDate;
}