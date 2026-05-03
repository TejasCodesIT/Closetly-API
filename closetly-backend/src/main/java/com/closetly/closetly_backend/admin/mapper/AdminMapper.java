package com.closetly.closetly_backend.admin.mapper;

import com.closetly.closetly_backend.admin.dto.*;
import com.closetly.closetly_backend.admin.entity.Report;
import com.closetly.closetly_backend.admin.entity.ReviewFlag;
import com.closetly.closetly_backend.admin.entity.SystemLog;
import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.review.entity.Review;

import java.util.List;

/**
 * Mapper utility for converting admin entities to DTOs
 */
public class AdminMapper {

    public static ReportedProductDTO toReportedProductDTO(Report report) {
        Product product = report.getProduct();
        String productImage = product.getPrimaryImageUrl();

        return ReportedProductDTO.builder()
                .id(report.getId())
                .productId(product.getId())
                .sellerId(product.getSeller().getId())
                .sellerName(product.getSeller().getFullName())
                .productTitle(product.getTitle())
                .reportCount(1) // Single report
                .reportReasons(List.of(report.getReason()))
                .status(report.getStatus().toString())
                .severity("MEDIUM") // Default severity
                .reportedAt(report.getReportedAt())
                .images(product.getImageUrls())
                .build();
    }

    public static ReviewModerationDTO toReviewModerationDTO(ReviewFlag flag) {
        Review review = flag.getReview();
        return ReviewModerationDTO.builder()
                .id(flag.getId())
                .reviewId(review.getId())
                .productId(review.getProduct().getId())
                .productTitle(review.getProduct().getTitle())
                .reviewerName(review.getReviewer().getFullName())
                .rating(review.getRating())
                .reviewText(review.getComment())
                .flagReason(flag.getReason())
                .reportedFor(List.of(flag.getReason()))
                .status(flag.getStatus().toString())
                .flaggedAt(flag.getFlaggedAt())
                .build();
    }

    public static SystemLogDTO toSystemLogDTO(SystemLog log) {
        return SystemLogDTO.builder()
                .id(log.getId())
                .timestamp(log.getCreatedAt())
                .level("INFO") // Default level for admin logs
                .module("ADMIN")
                .message(log.getMessage())
                .action(log.getType().toString())
                .status("COMPLETED")
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
