package com.closetly.closetly_backend.admin.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLogDTO {
    private Long id;
    private String type;
    private String message;
    private String details;
    private LocalDateTime createdAt;
}
