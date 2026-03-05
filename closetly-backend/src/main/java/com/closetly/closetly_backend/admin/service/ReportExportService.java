package com.closetly.closetly_backend.admin.service;

import com.closetly.closetly_backend.admin.dto.ReportExportDTO;
import com.closetly.closetly_backend.admin.entity.Report;
import com.closetly.closetly_backend.admin.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportExportService {

    private final ReportRepository reportRepository;

    public String exportReportsToCsv(String type) throws IOException {
        List<Report> reports = getReportsByType(type);
        List<ReportExportDTO> exportDTOs = reports.stream()
                .map(this::mapToExportDTO)
                .collect(Collectors.toList());

        return generateCsv(exportDTOs);
    }

    private List<Report> getReportsByType(String type) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        switch (type.toLowerCase()) {
            case "monthly":
                startDate = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                break;
            case "yearly":
                startDate = now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);
                break;
            case "weekly":
                startDate = now.minusWeeks(1);
                break;
            default:
                startDate = now.minusMonths(1);
        }

        return reportRepository.findAll().stream()
                .filter(r -> r.getReportedAt().isAfter(startDate))
                .collect(Collectors.toList());
    }

    private ReportExportDTO mapToExportDTO(Report report) {
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

    private String generateCsv(List<ReportExportDTO> data) throws IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // Header
        pw.println(
                "Report ID,Product ID,Product Title,Seller Username,Reason,Status,Reported At,Approved At,Admin Notes");

        // Data
        for (ReportExportDTO dto : data) {
            pw.println(String.format(
                    "%d,%d,\"%s\",\"%s\",\"%s\",%s,%s,%s,\"%s\"",
                    dto.getReportId(),
                    dto.getProductId(),
                    escapeCsv(dto.getProductTitle()),
                    escapeCsv(dto.getSellerUsername()),
                    escapeCsv(dto.getReason()),
                    dto.getStatus(),
                    dto.getReportedAt(),
                    dto.getApprovedAt(),
                    escapeCsv(dto.getAdminNotes())));
        }

        pw.flush();
        return sw.toString();
    }

    private String escapeCsv(String value) {
        if (value == null)
            return "";
        return value.replace("\"", "\"\"");
    }
}
