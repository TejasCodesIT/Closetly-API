package com.closetly.closetly_backend.admin.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewModerationDTO {
    private Long reviewId;
    private Long productId;
    private String productTitle;
    private Long reviewerId;
    private String reviewerUsername;
    private Integer rating;
    private String comment;
    private String flagReason;
    private String flagStatus;
    private LocalDateTime reviewedAt;
}
