package com.closetly.closetly_backend.order.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDTO {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private Long sellerId;
    private String sellerName;
    private String sellerEmail;
    private List<OrderItemDTO> orderItems;
    private Double totalAmount;
    private Integer totalItems;
    private String status;
    private LocalDateTime createdAt;
}