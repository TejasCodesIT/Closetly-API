package com.closetly.closetly_backend.notification.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private Long id;
    private Long userId;
    private String content;
    private boolean seen;
    private LocalDateTime createdAt;
}
