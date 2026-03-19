package com.closetly.closetly_backend.search.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchRequestDTO {
    @NotBlank
    private String query;
}

