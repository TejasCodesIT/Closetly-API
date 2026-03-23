package com.closetly.closetly_backend.booking.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingResponseDTO {
    private Long id;
    private Long productId;
    private String productTitle;
    private String productBrand;
    private String productImageUrl;
    private Long customerId;
    private Long sellerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String message;
}
