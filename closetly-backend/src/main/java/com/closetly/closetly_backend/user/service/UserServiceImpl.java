package com.closetly.closetly_backend.user.service;

import org.springframework.security.core.Authentication;
import com.closetly.closetly_backend.user.dto.AuthResponse;
import com.closetly.closetly_backend.user.dto.LoginRequest;
import com.closetly.closetly_backend.user.dto.RegistrationRequest;
import com.closetly.closetly_backend.user.dto.UserProfileDTO;
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

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final AuthenticationManager authenticationManager;
        private final JwtTokenProvider tokenProvider;
        private final RoleRepository roleRepository;

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
}
