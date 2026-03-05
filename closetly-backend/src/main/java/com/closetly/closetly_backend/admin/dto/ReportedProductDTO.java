package com.closetly.closetly_backend.admin.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportedProductDTO {
    private Long reportId;
    private Long productId;
    private String productTitle;
    private String productImage;
    private String sellerUsername;
    private String sellerEmail;
    private String reason;
    private String description;
    private String status;
    private LocalDateTime reportedAt;
}
