package com.closetly.closetly_backend.notification.controller;

import com.closetly.closetly_backend.notification.dto.NotificationDTO;
import com.closetly.closetly_backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/me")
    public ResponseEntity<List<NotificationDTO>> myNotifications(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getMyNotifications(authentication.getName()));
    }
}
