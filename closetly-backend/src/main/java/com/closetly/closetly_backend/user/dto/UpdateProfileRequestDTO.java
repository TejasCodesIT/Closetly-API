package com.closetly.closetly_backend.user.dto;

import lombok.Data;

@Data
public class UpdateProfileRequestDTO {
    private String fullName;
    private String phoneNumber;
    private Double latitude;
    private Double longitude;
}

