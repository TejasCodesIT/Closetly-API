package com.closetly.closetly_backend.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrGetChatByBookingRequestDTO {
    @NotNull(message = "bookingId is required")
    private Long bookingId;
}
