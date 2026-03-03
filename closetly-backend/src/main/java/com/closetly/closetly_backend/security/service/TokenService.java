package com.closetly.closetly_backend.security.service;

import com.closetly.closetly_backend.user.dto.AuthResponse;

public interface TokenService {
    AuthResponse generateTokens(Long userId);

    void revokeTokens(Long userId);
}
