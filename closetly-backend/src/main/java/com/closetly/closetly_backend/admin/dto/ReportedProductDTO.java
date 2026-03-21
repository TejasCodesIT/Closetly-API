package com.closetly.closetly_backend.admin.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportedProductDTO {
    private Long id;
    private Long productId;
    private Long sellerId;
    private String sellerName;
    private String productTitle;
    private Integer reportCount;
    private List<String> reportReasons;
    private String status;
    private String severity;
    private LocalDateTime reportedAt;
    private List<String> images;
}
