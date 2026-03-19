package com.closetly.closetly_backend.user.service;

import org.springframework.security.core.Authentication;
import com.closetly.closetly_backend.user.dto.AuthResponse;
import com.closetly.closetly_backend.user.dto.LoginRequest;
import com.closetly.closetly_backend.user.dto.RegistrationRequest;
import com.closetly.closetly_backend.user.dto.UpdateProfileRequestDTO;
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
import java.util.Objects;
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

                String verificationToken = UUID.randomUUID().toString();
                User user = User.builder()
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .fullName(request.getFullName())
                                .profileImageUrl(null)
                                .phoneNumber(null)
                                .latitude(request.getLatitude())
                                .longitude(request.getLongitude())
                                .enabled(false)
                                .emailVerified(false)
                                .verificationToken(verificationToken)
                                .roles(Set.of(userRole))
                                .build();
                user = Objects.requireNonNull(userRepository.save(user));

                emailService.sendEmailVerificationEmail(user.getEmail(), verificationToken);

                AuthResponse resp = new AuthResponse();
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
                Long id = Objects.requireNonNull(userId, "userId is required");
                User user = userRepository.findById(id)
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
                Long id = Objects.requireNonNull(userId, "userId is required");
                User user = userRepository.findById(id)
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

        @Override
        public void verifyEmail(String token) {
                if (token == null || token.trim().isEmpty()) {
                        throw new IllegalArgumentException("Verification token is required");
                }
                User user = userRepository.findByVerificationToken(token)
                                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));
                user.setEmailVerified(true);
                user.setEnabled(true);
                user.setVerificationToken(null);
                userRepository.save(user);
        }

        @Override
        public UserProfileDTO getProfileByEmail(String email) {
                if (email == null || email.trim().isEmpty()) {
                        throw new IllegalArgumentException("Email is required");
                }
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                return toProfileDto(user);
        }

        @Override
        public UserProfileDTO updateProfile(String email, UpdateProfileRequestDTO request) {
                if (email == null || email.trim().isEmpty()) {
                        throw new IllegalArgumentException("Email is required");
                }
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new IllegalArgumentException("User not found"));

                if (request.getFullName() != null) {
                        user.setFullName(request.getFullName());
                }
                if (request.getPhoneNumber() != null) {
                        user.setPhoneNumber(request.getPhoneNumber());
                }
                if (request.getLatitude() != null) {
                        user.setLatitude(request.getLatitude());
                }
                if (request.getLongitude() != null) {
                        user.setLongitude(request.getLongitude());
                }

                user = userRepository.save(user);
                return toProfileDto(user);
        }

        @Override
        public UserProfileDTO updateProfileImage(String email, String profileImageUrl) {
                if (email == null || email.trim().isEmpty()) {
                        throw new IllegalArgumentException("Email is required");
                }
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                user.setProfileImageUrl(profileImageUrl);
                user = userRepository.save(user);
                return toProfileDto(user);
        }

        @Override
        public void changePassword(String email, String currentPassword, String newPassword) {
                if (email == null || email.trim().isEmpty()) {
                        throw new IllegalArgumentException("Email is required");
                }
                if (currentPassword == null || currentPassword.isBlank()) {
                        throw new IllegalArgumentException("Current password is required");
                }
                if (newPassword == null || newPassword.isBlank()) {
                        throw new IllegalArgumentException("New password is required");
                }
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new IllegalArgumentException("User not found"));

                if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                        throw new IllegalArgumentException("Current password is incorrect");
                }
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
        }

        private UserProfileDTO toProfileDto(User user) {
                UserProfileDTO dto = new UserProfileDTO();
                dto.setId(user.getId());
                dto.setEmail(user.getEmail());
                dto.setFullName(user.getFullName());
                dto.setProfileImageUrl(user.getProfileImageUrl());
                dto.setPhoneNumber(user.getPhoneNumber());
                dto.setLatitude(user.getLatitude());
                dto.setLongitude(user.getLongitude());
                return dto;
        }
}
