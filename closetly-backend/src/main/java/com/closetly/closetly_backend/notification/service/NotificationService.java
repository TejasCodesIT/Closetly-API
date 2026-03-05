package com.closetly.closetly_backend.notification.service;

import com.closetly.closetly_backend.notification.dto.NotificationDTO;

import java.util.List;

public interface NotificationService {
    NotificationDTO createNotification(NotificationDTO dto);

    List<NotificationDTO> getNotificationsForUser(Long userId);
}
