package com.closetly.closetly_backend.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatRoomDTO {
    private Long id;
    private Long productId;
    private Long buyerId;
    private Long sellerId;
    private LocalDateTime createdAt;
}

