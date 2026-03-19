package com.closetly.closetly_backend.admin.controller;

import com.closetly.closetly_backend.admin.dto.*;
import com.closetly.closetly_backend.admin.entity.ReviewFlag;
import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.admin.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final DashboardService dashboardService;
    private final ReportService reportService;
    private final ReviewModerationService reviewModerationService;
    private final SystemLogService systemLogService;
    private final ReportExportService reportExportService;

    // ==================== Dashboard Overview API ====================

    /**
     * GET /api/admin/dashboard
     * Returns dashboard metrics
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardOverviewDTO> getDashboard() {
        DashboardOverviewDTO overview = dashboardService.getOverview();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Controller auth: " + auth);
        System.out.println("Controller authorities:*********************************** " + auth.getAuthorities());
        return ResponseEntity.ok(overview);
    }

    /**
     * GET /api/admin/dashboard/overview
     * Returns dashboard metrics (alternative endpoint)
     */
    @GetMapping("/dashboard/overview")
    public ResponseEntity<DashboardOverviewDTO> getDashboardOverview() {
        DashboardOverviewDTO overview = dashboardService.getOverview();
        return ResponseEntity.ok(overview);
    }

    /**
     * GET /api/admin/summary
     * Alias for dashboard overview (used by admin dashboard)
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardOverviewDTO> getSummary() {
        return ResponseEntity.ok(dashboardService.getOverview());
    }

    // ==================== Reported Products API ====================

    /**
     * GET /api/admin/reported-products
     * Returns paginated list of reported products
     */
    @GetMapping("/reported-products")
    public ResponseEntity<Page<ReportedProductDTO>> getReportedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportedProductDTO> reports = reportService.getReportedProducts(pageable);
        return ResponseEntity.ok(reports);
    }

    /**
     * GET /api/admin/reviews
     * Alias for review moderation list
     */
    @GetMapping("/reviews")
    public ResponseEntity<Page<ReviewModerationDTO>> getReviews(
            @RequestParam(required = false) ReviewFlag.FlagStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewModerationDTO> reviews = reviewModerationService.getFlaggedReviews(status, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * POST /api/admin/reports/{reportId}/approve
     * Approve a report and block the product
     */
    @PostMapping("/reports/{reportId}/approve")
    public ResponseEntity<String> approveReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportActionDTO actionDTO) {
        reportService.approveReport(reportId, actionDTO.getAdminNotes());
        return ResponseEntity.ok("Report approved successfully");
    }

    /**
     * POST /api/admin/reports/{reportId}/block
     * Block and delete the product
     */
    @PostMapping("/reports/{reportId}/block")
    public ResponseEntity<String> blockProduct(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportActionDTO actionDTO) {
        reportService.blockProduct(reportId, actionDTO.getAdminNotes());
        return ResponseEntity.ok("Product blocked successfully");
    }

    /**
     * POST /api/admin/reports/{reportId}/reject
     * Reject a report
     */
    @PostMapping("/reports/{reportId}/reject")
    public ResponseEntity<String> rejectReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportActionDTO actionDTO) {
        reportService.rejectReport(reportId, actionDTO.getAdminNotes());
        return ResponseEntity.ok("Report rejected successfully");
    }

    // ==================== Review Moderation API ====================

    /**
     * GET /api/admin/reviews?status=flagged
     * Returns flagged reviews with pagination
     */
    @GetMapping("/review-moderation")
    public ResponseEntity<Page<ReviewModerationDTO>> getFlaggedReviews(
            @RequestParam(required = false) ReviewFlag.FlagStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewModerationDTO> reviews = reviewModerationService.getFlaggedReviews(status, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * DELETE /api/admin/reviews/{reviewId}
     * Delete a review
     */
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<String> deleteReview(@PathVariable Long reviewId) {
        reviewModerationService.deleteReview(reviewId);
        return ResponseEntity.ok("Review deleted successfully");
    }

    /**
     * POST /api/admin/users/{userId}/ban
     * Ban a user
     */
    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<String> banUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserBanDTO banDTO) {
        reviewModerationService.banUser(userId, banDTO.getBanReason());
        return ResponseEntity.ok("User banned successfully. Reason: " + banDTO.getBanReason());
    }

    // ==================== System Logs API ====================

    /**
     * GET /api/admin/system-logs?limit=10
     * Returns system logs
     */
    @GetMapping("/system-logs")
    public ResponseEntity<Page<SystemLogDTO>> getSystemLogs(
            @RequestParam(defaultValue = "10") int limit) {
        Page<SystemLogDTO> logs = systemLogService.getLogs(limit);
        return ResponseEntity.ok(logs);
    }

    /**
     * GET /api/admin/logs
     * Alias for system logs
     */
    @GetMapping("/logs")
    public ResponseEntity<Page<SystemLogDTO>> getLogs(@RequestParam(defaultValue = "10") int limit) {
        return getSystemLogs(limit);
    }

    // ==================== Export Report API ====================

    /**
     * GET /api/admin/reports/export?type=monthly
     * Exports reports to CSV file
     * 
     * @param type monthly|yearly|weekly|all
     */
    @GetMapping("/reports/export")
    public ResponseEntity<String> exportReports(
            @RequestParam(defaultValue = "monthly") String type) {
        try {
            String csvContent = reportExportService.exportReportsToCsv(type);

            String filename = String.format("reports_%s_%s.csv",
                    type,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                    .body(csvContent);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Error generating report: " + e.getMessage());
        }
    }

    /**
     * GET /api/admin/export
     * Alias for reports export (defaults to monthly)
     */
    @GetMapping("/export")
    public ResponseEntity<String> exportDefault() {
        return exportReports("monthly");
    }

    /**
     * PATCH /api/admin/products/{id}/status
     * Update product status (ACTIVE, INACTIVE, BLOCKED)
     */
    @PatchMapping("/products/{productId}/status")
    public ResponseEntity<String> updateProductStatus(
            @PathVariable Long productId,
            @RequestParam Product.ProductStatus status) {
        // reuse ReportService's productRepository via a small helper would be better,
        // but for now delegate through reportService where appropriate
        // Here we directly update via ProductService/Repository if needed; keeping simple:
        // In this codebase, ReportService already manages status changes via reports.
        // For admin manual override, we can call productRepository via a new service method.
        // To avoid larger refactor, we treat BLOCKED via a dummy report path is overkill,
        // so instead update via a minimal inline service (omitted here for brevity).
        return ResponseEntity.status(501).body("Manual product status update not yet wired");
    }

    /**
     * PATCH /api/admin/reviews/{id}/status
     * Update review flag status (PENDING, APPROVED, REJECTED)
     */
    @PatchMapping("/reviews/{reviewFlagId}/status")
    public ResponseEntity<String> updateReviewStatus(
            @PathVariable Long reviewFlagId,
            @RequestParam ReviewFlag.FlagStatus status) {
        reviewModerationService.updateReviewFlagStatus(reviewFlagId, status);
        return ResponseEntity.ok("Review flag status updated to " + status);
    }
}
