package com.closetly.closetly_backend.user.service;

import com.closetly.closetly_backend.user.dto.AuthResponse;
import com.closetly.closetly_backend.user.dto.LoginRequest;
import com.closetly.closetly_backend.user.dto.RegistrationRequest;
import com.closetly.closetly_backend.user.dto.UserProfileDTO;
import com.closetly.closetly_backend.user.dto.ForgotPasswordRequest;
import com.closetly.closetly_backend.user.dto.ResetPasswordRequest;

public interface UserService {
    AuthResponse register(RegistrationRequest request);

    AuthResponse login(LoginRequest request);

    UserProfileDTO getProfile(Long userId);

    void assignRole(Long userId, String roleName);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    boolean validateResetToken(String token);

    void verifyEmail(String token);

    UserProfileDTO getProfileByEmail(String email);

    UserProfileDTO updateProfile(String email, com.closetly.closetly_backend.user.dto.UpdateProfileRequestDTO request);

    UserProfileDTO updateProfileImage(String email, String profileImageUrl);

    void changePassword(String email, String currentPassword, String newPassword);

    // additional methods like update, find by id etc
}
