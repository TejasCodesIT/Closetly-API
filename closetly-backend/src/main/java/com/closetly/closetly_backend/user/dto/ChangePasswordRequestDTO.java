package com.closetly.closetly_backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequestDTO {
    @NotBlank
    private String currentPassword;

    @NotBlank
    private String newPassword;
}

