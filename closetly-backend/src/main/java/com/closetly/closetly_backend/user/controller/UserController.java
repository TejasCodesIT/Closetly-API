package com.closetly.closetly_backend.user.controller;

import com.closetly.closetly_backend.user.dto.ChangePasswordRequestDTO;
import com.closetly.closetly_backend.user.dto.UpdateProfileRequestDTO;
import com.closetly.closetly_backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final com.closetly.closetly_backend.user.service.ProfileImageStorageService profileImageStorageService;

    /**
     * POST /api/users/{userId}/roles/{roleName}
     * Assign a role to a user (ADMIN only)
     */
    @PostMapping("/{userId}/roles/{roleName}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> assignRoleToUser(
            @PathVariable Long userId,
            @PathVariable String roleName) {
        try {
            userService.assignRole(userId, roleName);
            return ResponseEntity.ok().body("Role '" + roleName + "' assigned to user " + userId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequestDTO request, Authentication authentication) {
        return ResponseEntity.ok(userService.updateProfile(authentication.getName(), request));
    }

    @PostMapping("/upload-profile-image")
    public ResponseEntity<?> uploadProfileImage(@RequestParam("file") MultipartFile file,
            Authentication authentication) {
        var profile = userService.getProfileByEmail(authentication.getName());
        String url = profileImageStorageService.saveProfileImage(file, profile.getId());
        return ResponseEntity.ok(userService.updateProfileImage(authentication.getName(), url));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request,
            Authentication authentication) {
        userService.changePassword(authentication.getName(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok().body(java.util.Map.of("message", "Password changed successfully"));
    }
}
