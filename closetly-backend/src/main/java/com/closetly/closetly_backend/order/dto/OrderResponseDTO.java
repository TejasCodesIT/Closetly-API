package com.closetly.closetly_backend.order.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderResponseDTO {
    private Long id;
    private Long productId;
    private Long customerId;
    private Double price;
    private String status;
    private LocalDateTime createdAt;
}

