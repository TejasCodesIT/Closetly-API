package com.closetly.closetly_backend.order.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class OrderItemDTO {
    private Long id;
    private Long productId;
    private String productTitle;
    private String productBrand;
    private String productImage;
    private Integer quantity;
    private Double unitPrice;
    private Double totalPrice;
    private String type; // BUY or RENT
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer rentalDays;
    private LocalDateTime createdAt;
}