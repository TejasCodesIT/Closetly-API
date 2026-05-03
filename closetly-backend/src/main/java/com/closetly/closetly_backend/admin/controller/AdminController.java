package com.closetly.closetly_backend.admin.controller;

import com.closetly.closetly_backend.admin.dto.*;
import com.closetly.closetly_backend.admin.entity.ReviewFlag;
import com.closetly.closetly_backend.admin.service.*;
import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.support.service.SupportRequestService;
import com.closetly.closetly_backend.support.entity.SupportRequest;
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
    private final AdminService adminService;
    private final SupportRequestService supportRequestService;

    // ==================== Dashboard Overview API ====================

    /**
     * GET /api/admin/dashboard
     * Returns dashboard metrics
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardOverviewDTO> getDashboard() {
        DashboardOverviewDTO overview = adminService.getDashboardOverview();
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
        DashboardOverviewDTO overview = adminService.getDashboardOverview();
        return ResponseEntity.ok(overview);
    }

    /**
     * GET /api/admin/summary
     * Alias for dashboard overview (used by admin dashboard)
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardOverviewDTO> getSummary() {
        return ResponseEntity.ok(adminService.getDashboardOverview());
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

    // ==================== Users Management API ====================

    /**
     * GET /api/admin/users
     * Returns paginated list of all users
     */
    @GetMapping("/users")
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserDTO> users = adminService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * POST /api/admin/users/{userId}/block
     * Block a user
     */
    @PostMapping("/users/{userId}/block")
    public ResponseEntity<String> blockUser(@PathVariable Long userId) {
        adminService.blockUser(userId);
        return ResponseEntity.ok("User blocked successfully");
    }

    /**
     * POST /api/admin/users/{userId}/unblock
     * Unblock a user
     */
    @PostMapping("/users/{userId}/unblock")
    public ResponseEntity<String> unblockUser(@PathVariable Long userId) {
        adminService.unblockUser(userId);
        return ResponseEntity.ok("User unblocked successfully");
    }

    /**
     * DELETE /api/admin/users/{userId}
     * Delete a user
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok("User deleted successfully");
    }

    // ==================== Products Management API ====================

    /**
     * GET /api/admin/products
     * Returns paginated list of all products
     */
    @GetMapping("/products")
    public ResponseEntity<Page<ProductDTO>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDTO> products = adminService.getAllProducts(pageable);
        return ResponseEntity.ok(products);
    }

    /**
     * PUT /api/admin/products/{productId}/approve
     * Approve a product
     */
    @PutMapping("/products/{productId}/approve")
    public ResponseEntity<String> approveProduct(@PathVariable Long productId) {
        adminService.restoreProduct(productId);
        return ResponseEntity.ok("Product approved successfully");
    }

    /**
     * PUT /api/admin/products/{productId}/reject
     * Reject a product
     */
    @PutMapping("/products/{productId}/reject")
    public ResponseEntity<String> rejectProduct(@PathVariable Long productId) {
        adminService.deleteProduct(productId);
        return ResponseEntity.ok("Product rejected successfully");
    }

    /**
     * PUT /api/admin/products/{productId}/block
     * Block a product
     */
    @PutMapping("/products/{productId}/block")
    public ResponseEntity<String> blockProduct(@PathVariable Long productId) {
        adminService.blockProduct(productId);
        return ResponseEntity.ok("Product blocked successfully");
    }

    /**
     * DELETE /api/admin/products/{productId}
     * Delete a product
     */
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long productId) {
        adminService.deleteProduct(productId);
        return ResponseEntity.ok("Product deleted successfully");
    }

    /**
     * PUT /api/admin/products/{productId}/restore
     * Restore a deleted product
     */
    @PutMapping("/products/{productId}/restore")
    public ResponseEntity<String> restoreProduct(@PathVariable Long productId) {
        adminService.restoreProduct(productId);
        return ResponseEntity.ok("Product restored successfully");
    }

    // ==================== Bookings Management API ====================

    /**
     * GET /api/admin/bookings
     * Returns paginated list of all bookings
     */
    @GetMapping("/bookings")
    public ResponseEntity<Page<BookingDTO>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BookingDTO> bookings = adminService.getAllBookings(pageable);

        // DEBUG: Verify data is being sent
        if (!bookings.isEmpty()) {
            BookingDTO first = bookings.getContent().get(0);
            System.out.println("[DEBUG] First booking DTO: id=" + first.getId() +
                    ", productTitle=" + first.getProductTitle() +
                    ", productBrand=" + first.getProductBrand() +
                    ", productImageUrl=" + first.getProductImageUrl());
        }

        return ResponseEntity.ok(bookings);
    }

    /**
     * PUT /api/admin/bookings/{bookingId}/cancel
     * Cancel a booking
     */
    @PutMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<String> cancelBooking(@PathVariable Long bookingId) {
        adminService.cancelBooking(bookingId);
        return ResponseEntity.ok("Booking cancelled successfully");
    }

    // ==================== Requests Management API ====================

    /**
     * GET /api/admin/requests
     * Returns paginated list of all requests (support requests)
     */
    @GetMapping("/requests")
    public ResponseEntity<Page<RequestDTO>> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SupportRequest> supportRequests = supportRequestService.getAllSupportRequests(pageable);
        Page<RequestDTO> requestDTOs = supportRequests.map(this::mapSupportRequestToRequestDTO);
        return ResponseEntity.ok(requestDTOs);
    }

    // ==================== Export Report API ====================

    /**
     * POST /api/admin/export
     * Export reports in different formats (PDF, CSV, XLSX)
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportReport(@RequestBody ExportRequestDTO request) {
        try {
            byte[] fileData;
            String contentType;
            String filename;

            switch (request.getFormat().toLowerCase()) {
                case "csv":
                    String csvContent = reportExportService.exportReportsToCsv("monthly"); // Default to monthly for now
                    fileData = csvContent.getBytes();
                    contentType = "text/csv";
                    filename = "reports.csv";
                    break;
                case "pdf":
                    // For now, return CSV as PDF placeholder
                    String pdfContent = reportExportService.exportReportsToCsv("monthly");
                    fileData = pdfContent.getBytes();
                    contentType = "application/pdf";
                    filename = "reports.pdf";
                    break;
                case "xlsx":
                    // For now, return CSV as XLSX placeholder
                    String xlsxContent = reportExportService.exportReportsToCsv("monthly");
                    fileData = xlsxContent.getBytes();
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "reports.xlsx";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported format: " + request.getFormat());
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileData.length))
                    .body(fileData);

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(("Error generating report: " + e.getMessage()).getBytes());
        }
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
        // Here we directly update via ProductService/Repository if needed; keeping
        // simple:
        // In this codebase, ReportService already manages status changes via reports.
        // For admin manual override, we can call productRepository via a new service
        // method.
        // To avoid larger refactor, we treat BLOCKED via a dummy report path is
        // overkill,
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

    /**
     * Helper method to map SupportRequest to RequestDTO
     */
    private RequestDTO mapSupportRequestToRequestDTO(SupportRequest supportRequest) {
        return RequestDTO.builder()
                .id(supportRequest.getId())
                .type("HELP") // All support requests are help requests
                .description(supportRequest.getSubject() + ": " + supportRequest.getMessage())
                .userId(null) // Support requests don't have user IDs in current implementation
                .userName(supportRequest.getName())
                .status(supportRequest.getStatus().name())
                .createdAt(supportRequest.getCreatedAt())
                .resolvedAt(null) // Not tracking resolution time yet
                .build();
    }
}
