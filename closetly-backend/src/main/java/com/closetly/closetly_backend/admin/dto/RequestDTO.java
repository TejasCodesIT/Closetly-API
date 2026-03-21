package com.closetly.closetly_backend.admin.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestDTO {
    private Long id;
    private String type;
    private String description;
    private Long userId;
    private String userName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}