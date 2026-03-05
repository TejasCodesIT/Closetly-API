package com.closetly.closetly_backend.admin.dto;

import lombok.*;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBanDTO {
    @NotBlank(message = "Ban reason is required")
    private String banReason;

    private String banDetails;
}
