package com.closetly.closetly_backend.admin.dto;

import lombok.Data;

@Data
public class ExportRequestDTO {
    private String format; // "pdf", "csv", "xlsx"
    private String[] sections; // "summary", "products", "reviews", "logs"
    private String dateRange; // Optional date range
}