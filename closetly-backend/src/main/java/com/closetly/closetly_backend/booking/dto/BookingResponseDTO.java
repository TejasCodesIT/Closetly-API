package com.closetly.closetly_backend.booking.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingResponseDTO {
    private Long id;
    private Long productId;
    private Long customerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String message;
}
