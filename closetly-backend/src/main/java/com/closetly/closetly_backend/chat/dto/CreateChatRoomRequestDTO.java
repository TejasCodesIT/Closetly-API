package com.closetly.closetly_backend.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateChatRoomRequestDTO {
    @NotNull(message = "productId is required")
    private Long productId;

    // If present, chat room is for an approved rental booking.
    private Long bookingId;

    // If present, chat room is for a purchased order.
    private Long orderId;

    // Optional: client-provided buyerId/sellerId (backend can also derive these).
    private Long buyerId;
    private Long sellerId;
}

