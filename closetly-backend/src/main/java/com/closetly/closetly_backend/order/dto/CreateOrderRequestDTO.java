package com.closetly.closetly_backend.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequestDTO {
    @NotEmpty(message = "Order items cannot be empty")
    @NotNull(message = "Order items are required")
    private List<OrderItemRequestDTO> items;
}