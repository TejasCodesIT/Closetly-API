package com.closetly.closetly_backend.admin.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExportDTO {
    private Long reportId;
    private Long productId;
    private String productTitle;
    private String sellerUsername;
    private String reason;
    private String status;
    private String reportedAt;
    private String approvedAt;
    private String adminNotes;
}
