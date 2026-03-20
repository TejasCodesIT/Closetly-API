package com.closetly.closetly_backend.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateChatRoomRequestDTO {
    @NotNull(message = "productId is required")
    private Long productId;

    // @NotNull(message = "buyerId is required")
    // private Long buyerId;
}

