package com.closetly.closetly_backend.user.controller;

import com.closetly.closetly_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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
}
