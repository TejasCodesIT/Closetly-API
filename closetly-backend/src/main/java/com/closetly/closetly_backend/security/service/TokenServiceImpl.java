package com.closetly.closetly_backend.security.service;

import com.closetly.closetly_backend.security.JwtTokenProvider;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import com.closetly.closetly_backend.user.dto.AuthResponse;
import com.closetly.closetly_backend.security.entity.RefreshToken;
import com.closetly.closetly_backend.security.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    @Override
    public AuthResponse generateTokens(Long userId) {
        User user = userRepository.findByIdIncludingDeleted(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        String access = tokenProvider.generateToken(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user.getEmail(),
                        ""));
        String refresh = UUID.randomUUID().toString();
        RefreshToken token = RefreshToken.builder()
                .token(refresh)
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(30))
                .build();
        refreshTokenRepository.save(token);
        AuthResponse resp = new AuthResponse();
        resp.setAccessToken(access);
        resp.setRefreshToken(refresh);
        return resp;
    }

    @Override
    public void revokeTokens(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
