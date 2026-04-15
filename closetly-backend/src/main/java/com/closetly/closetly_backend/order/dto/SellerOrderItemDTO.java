package com.closetly.closetly_backend.order.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SellerOrderItemDTO {
    private Long orderItemId;
    private Long orderId;
    private Long productId;
    private String productName;
    private String productBrand;
    private String image;
    private Integer quantity;
    private Double price;
    private String type; // BUY or RENT
    private String orderStatus;
    private LocalDateTime createdAt;
    private String customerName;
    private String customerEmail;
}