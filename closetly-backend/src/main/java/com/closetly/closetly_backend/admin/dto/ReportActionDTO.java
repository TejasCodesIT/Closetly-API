package com.closetly.closetly_backend.admin.dto;

import lombok.*;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportActionDTO {
    @NotBlank(message = "Admin notes required")
    private String adminNotes;
}
