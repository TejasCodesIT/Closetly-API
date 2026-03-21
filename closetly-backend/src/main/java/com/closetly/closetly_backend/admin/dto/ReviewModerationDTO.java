package com.closetly.closetly_backend.admin.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewModerationDTO {

    private Long id;
    private Long reviewId;
    private Long productId;

    private String productTitle;
    private String reviewerName;

    private Integer rating;
    private String reviewText;

    private String flagReason;
    private List<String> reportedFor;

    private String status;
    private LocalDateTime flaggedAt;
}