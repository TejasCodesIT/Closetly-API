package com.closetly.closetly_backend.user.controller;

import com.closetly.closetly_backend.security.CustomUserDetails;
import com.closetly.closetly_backend.user.dto.AuthResponse;
import com.closetly.closetly_backend.user.dto.LoginRequest;
import com.closetly.closetly_backend.user.dto.RegistrationRequest;
import com.closetly.closetly_backend.user.dto.ForgotPasswordRequest;
import com.closetly.closetly_backend.user.dto.ResetPasswordRequest;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegistrationRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Not authenticated");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        User user = userDetails.getUser();

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());
        response.put("roles", user.getRoles());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            userService.forgotPassword(request);
            Map<String, String> response = new HashMap<>();
            response.put("message", "If the email exists, a reset link has been sent.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log the exception for debugging
            System.err.println("Error in forgot password: " + e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Unable to send email. Please try later.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            userService.resetPassword(request);
            return ResponseEntity.ok("Password reset successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/reset-password")
    public ResponseEntity<?> showResetPasswordForm(@RequestParam String token) {
        try {
            // Validate token exists and is not expired
            boolean isValid = userService.validateResetToken(token);
            if (isValid) {
                return ResponseEntity.ok(Map.of("valid", true, "message", "Token is valid"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("valid", false, "message", "Invalid or expired token"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", "Invalid token"));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, String> response = new HashMap<>();
        response.put("message", "Validation failed: " + errors.toString());
        return ResponseEntity.badRequest().body(response);
    }
}
