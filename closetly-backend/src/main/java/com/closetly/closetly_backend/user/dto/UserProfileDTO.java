package com.closetly.closetly_backend.user.dto;

import lombok.Data;

@Data
public class UserProfileDTO {
    private Long id;
    private String email;
    private String fullName;
    private Double latitude;
    private Double longitude;
}
