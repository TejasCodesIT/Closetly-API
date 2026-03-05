package com.closetly.closetly_backend.admin.mapper;

import com.closetly.closetly_backend.admin.dto.*;
import com.closetly.closetly_backend.admin.entity.Report;
import com.closetly.closetly_backend.admin.entity.ReviewFlag;
import com.closetly.closetly_backend.admin.entity.SystemLog;
import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.review.entity.Review;

/**
 * Mapper utility for converting admin entities to DTOs
 */
public class AdminMapper {

    public static ReportedProductDTO toReportedProductDTO(Report report) {
        Product product = report.getProduct();
        String productImage = product.getImages() != null && !product.getImages().isEmpty()
                ? product.getImages().get(0)
                : null;

        return ReportedProductDTO.builder()
                .reportId(report.getId())
                .productId(product.getId())
                .productTitle(product.getTitle())
                .productImage(productImage)
                .sellerUsername(product.getSeller().getFullName())
                .sellerEmail(product.getSeller().getEmail())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus().toString())
                .reportedAt(report.getReportedAt())
                .build();
    }

    public static ReviewModerationDTO toReviewModerationDTO(ReviewFlag flag) {
        Review review = flag.getReview();
        return ReviewModerationDTO.builder()
                .reviewId(review.getId())
                .productId(review.getProduct().getId())
                .productTitle(review.getProduct().getTitle())
                .reviewerId(review.getReviewer().getId())
                .reviewerUsername(review.getReviewer().getFullName())
                .rating(review.getRating())
                .comment(review.getComment())
                .flagReason(flag.getReason())
                .flagStatus(flag.getStatus().toString())
                .reviewedAt(review.getCreatedAt())
                .build();
    }

    public static SystemLogDTO toSystemLogDTO(SystemLog log) {
        return SystemLogDTO.builder()
                .id(log.getId())
                .type(log.getType().toString())
                .message(log.getMessage())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }

    public static ReportExportDTO toReportExportDTO(Report report) {
        return ReportExportDTO.builder()
                .reportId(report.getId())
                .productId(report.getProduct().getId())
                .productTitle(report.getProduct().getTitle())
                .sellerUsername(report.getProduct().getSeller().getFullName())
                .reason(report.getReason())
                .status(report.getStatus().toString())
                .reportedAt(report.getReportedAt().toString())
                .approvedAt(report.getUpdatedAt() != null ? report.getUpdatedAt().toString() : "")
                .adminNotes(report.getAdminNotes() != null ? report.getAdminNotes() : "")
                .build();
    }
}
