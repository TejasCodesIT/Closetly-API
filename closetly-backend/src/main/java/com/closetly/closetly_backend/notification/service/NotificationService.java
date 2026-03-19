package com.closetly.closetly_backend.notification.service;

import com.closetly.closetly_backend.notification.dto.NotificationDTO;

import java.util.List;

public interface NotificationService {
    void notifyUserByEmail(String email, String message);

    List<NotificationDTO> getMyNotifications(String email);
}
