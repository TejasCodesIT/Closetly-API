package com.closetly.closetly_backend.user.dto;

import com.closetly.closetly_backend.user.entity.Role;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserProfileDTO {
    private Long id;
    private String email;
    private String fullName;
    private String profileImageUrl;
    private String phoneNumber;
    private Double latitude;
    private Double longitude;
    private Set<Role> roles;
    private boolean enabled;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
