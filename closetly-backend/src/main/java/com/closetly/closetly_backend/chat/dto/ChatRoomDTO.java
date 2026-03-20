package com.closetly.closetly_backend.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatRoomDTO {
    private Long id;
    private Long productId;
    private Long buyerId;
    private Long sellerId;
    private String name;         // 👈 other user name
    private String lastMessage;  // 👈 preview
    private String time;         // 👈 last message time
    private LocalDateTime createdAt;
}

