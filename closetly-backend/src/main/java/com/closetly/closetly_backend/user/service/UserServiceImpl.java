package com.closetly.closetly_backend.user.service;

import org.springframework.security.core.Authentication;
import com.closetly.closetly_backend.user.dto.AuthResponse;
import com.closetly.closetly_backend.user.dto.LoginRequest;
import com.closetly.closetly_backend.user.dto.RegistrationRequest;
import com.closetly.closetly_backend.user.dto.UserProfileDTO;
import com.closetly.closetly_backend.user.dto.ForgotPasswordRequest;
import com.closetly.closetly_backend.user.dto.ResetPasswordRequest;
import com.closetly.closetly_backend.security.JwtTokenProvider;
import com.closetly.closetly_backend.user.entity.Role;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import com.closetly.closetly_backend.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final AuthenticationManager authenticationManager;
        private final JwtTokenProvider tokenProvider;
        private final RoleRepository roleRepository;
        private final EmailService emailService;

        @Override
        public AuthResponse register(RegistrationRequest request) {
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new IllegalArgumentException("Email already in use");
                }
                Role userRole = roleRepository.findByName(Role.RoleName.USER)
                                .orElseThrow(() -> new IllegalArgumentException("Default role not found"));
                User user = User.builder()
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .fullName(request.getFullName())
                                .latitude(request.getLatitude())
                                .longitude(request.getLongitude())
                                .enabled(true)
                                .roles(Set.of(userRole))
                                .build();
                userRepository.save(user);
                String access = tokenProvider.generateToken(
                                authenticationManager.authenticate(
                                                new UsernamePasswordAuthenticationToken(request.getEmail(),
                                                                request.getPassword())));
                AuthResponse resp = new AuthResponse();
                resp.setAccessToken(access);
                // refresh token generation could be added later
                return resp;
        }

        @Override
        public AuthResponse login(LoginRequest request) {

                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getEmail(),
                                                request.getPassword()));

                String access = tokenProvider.generateToken(authentication);

                AuthResponse resp = new AuthResponse();
                resp.setAccessToken(access);
                return resp;
        }

        @Override
        public UserProfileDTO getProfile(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                UserProfileDTO dto = new UserProfileDTO();
                dto.setId(user.getId());
                dto.setEmail(user.getEmail());
                dto.setFullName(user.getFullName());
                dto.setLatitude(user.getLatitude());
                dto.setLongitude(user.getLongitude());
                return dto;
        }

        @Override
        public void assignRole(Long userId, String roleName) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                Role role = roleRepository.findByName(Role.RoleName.valueOf(roleName.toUpperCase()))
                                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
                user.getRoles().add(role);
                userRepository.save(user);
        }

        @Override
        public void forgotPassword(ForgotPasswordRequest request) {
                // Check if user exists, but don't throw exception to avoid email enumeration
                Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

                if (userOptional.isPresent()) {
                        User user = userOptional.get();

                        // Generate reset token
                        String resetToken = UUID.randomUUID().toString();
                        user.setResetToken(resetToken);
                        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15)); // Token valid for 15 minutes
                        userRepository.save(user);

                        // Send email with reset token
                        try {
                                emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
                                System.out.println("Password reset email sent successfully to: " + user.getEmail());
                        } catch (Exception e) {
                                System.err.println("Failed to send password reset email to " + user.getEmail() + ": "
                                                + e.getMessage());
                                throw new RuntimeException("Unable to send email. Please try later.");
                        }
                } else {
                        // User doesn't exist, but we don't reveal this for security
                        System.out.println("Password reset requested for non-existent email: " + request.getEmail());
                }
        }

        @Override
        public void resetPassword(ResetPasswordRequest request) {
                User user = userRepository.findByResetToken(request.getToken())
                                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

                if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
                        throw new IllegalArgumentException("Reset token has expired");
                }

                user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                user.setResetToken(null);
                user.setResetTokenExpiry(null);
                userRepository.save(user);
        }

        @Override
        public boolean validateResetToken(String token) {
                Optional<User> userOptional = userRepository.findByResetToken(token);
                if (userOptional.isPresent()) {
                        User user = userOptional.get();
                        return user.getResetTokenExpiry().isAfter(LocalDateTime.now());
                }
                return false;
        }
}
