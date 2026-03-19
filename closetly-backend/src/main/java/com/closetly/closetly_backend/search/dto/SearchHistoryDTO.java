package com.closetly.closetly_backend.search.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchHistoryDTO {
    private Long id;
    private String query;
    private LocalDateTime timestamp;
}

