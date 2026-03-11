package com.closetly.closetly_backend.user.service;

import com.closetly.closetly_backend.user.dto.AuthResponse;
import com.closetly.closetly_backend.user.dto.LoginRequest;
import com.closetly.closetly_backend.user.dto.RegistrationRequest;
import com.closetly.closetly_backend.user.dto.UserProfileDTO;

public interface UserService {
    AuthResponse register(RegistrationRequest request);

    AuthResponse login(LoginRequest request);

    UserProfileDTO getProfile(Long userId);

    void assignRole(Long userId, String roleName);
    // additional methods like update, find by id etc
}
