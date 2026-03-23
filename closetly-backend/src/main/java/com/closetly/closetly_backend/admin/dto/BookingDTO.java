package com.closetly.closetly_backend.admin.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {
    private Long id;
    private Long productId;
    private String productTitle;
    private String productBrand;
    private String productImageUrl;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime createdAt;
}