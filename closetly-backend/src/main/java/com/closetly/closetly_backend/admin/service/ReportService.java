package com.closetly.closetly_backend.admin.service;

import com.closetly.closetly_backend.admin.dto.ReportedProductDTO;
import com.closetly.closetly_backend.admin.entity.Report;
import com.closetly.closetly_backend.admin.entity.SystemLog;
import com.closetly.closetly_backend.admin.repository.ReportRepository;
import com.closetly.closetly_backend.admin.repository.SystemLogRepository;
import com.closetly.closetly_backend.common.ResourceNotFoundException;
import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ProductRepository productRepository;
    private final SystemLogRepository systemLogRepository;

    @Transactional(readOnly = true)
    public Page<ReportedProductDTO> getReportedProducts(Pageable pageable) {
        return reportRepository.findByStatusOrderByReportedAtDesc(Report.ReportStatus.PENDING, pageable)
                .map(this::mapToDTO);
    }

    @Transactional
    public void approveReport(Long reportId, String adminNotes) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.setStatus(Report.ReportStatus.APPROVED);
        report.setAdminNotes(adminNotes);
        reportRepository.save(report);

        Product product = report.getProduct();
        product.setStatus(Product.ProductStatus.BLOCKED);
        productRepository.save(product);

        logAction(SystemLog.LogType.REPORT_APPROVED,
                "Report " + reportId + " approved. Product " + product.getId() + " blocked.");
    }

    @Transactional
    public void blockProduct(Long reportId, String adminNotes) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.setStatus(Report.ReportStatus.BLOCKED);
        report.setAdminNotes(adminNotes);
        reportRepository.save(report);

        Product product = report.getProduct();
        product.setStatus(Product.ProductStatus.BLOCKED);
        product.setDeleted(true);
        productRepository.save(product);

        logAction(SystemLog.LogType.PRODUCT_BLOCKED,
                "Product " + product.getId() + " permanently blocked based on report " + reportId);
    }

    @Transactional
    public void rejectReport(Long reportId, String adminNotes) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.setStatus(Report.ReportStatus.REJECTED);
        report.setAdminNotes(adminNotes);
        reportRepository.save(report);

        logAction(SystemLog.LogType.REPORT_REJECTED,
                "Report " + reportId + " rejected by admin.");
    }

    private ReportedProductDTO mapToDTO(Report report) {
        Product product = report.getProduct();
        String productImage = product.getImages() != null && !product.getImages().isEmpty()
                ? product.getImages().get(0)
                : null;

        return ReportedProductDTO.builder()
        .id(report.getId())
        .productId(product.getId())
        .sellerId(product.getSeller() != null ? product.getSeller().getId() : null)
        .sellerName(product.getSeller() != null ? product.getSeller().getFullName() : null)
        .productTitle(product.getTitle())
        .reportCount(1) // or calculate if multiple reports
        .reportReasons(java.util.List.of(report.getReason()))
        .status(report.getStatus().toString())
        .severity("LOW") // or dynamic
        .reportedAt(report.getReportedAt())
        .images(productImage != null ? java.util.List.of(productImage) : null)
        .build();

}
    private void logAction(SystemLog.LogType type, String message) {
        SystemLog log = SystemLog.builder()
                .type(type)
                .message(message)
                .build();
        systemLogRepository.save(log);
    }
}
