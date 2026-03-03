package com.closetly.closetly_backend.notification.service;

import com.closetly.closetly_backend.notification.dto.NotificationDTO;
import com.closetly.closetly_backend.notification.entity.Notification;
import com.closetly.closetly_backend.notification.repository.NotificationRepository;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public NotificationDTO createNotification(NotificationDTO dto) {
        // basic validation
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Notification n = new Notification();
        n.setUser(user);
        n.setContent(dto.getContent());
        n.setSeen(dto.isSeen());
        Notification saved = notificationRepository.save(n);
        dto.setId(saved.getId());
        dto.setCreatedAt(saved.getCreatedAt());
        return dto;
    }

    @Override
    public List<NotificationDTO> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserId(userId).stream()
                .map(n -> {
                    NotificationDTO dto = new NotificationDTO();
                    dto.setId(n.getId());
                    dto.setUserId(n.getUser().getId());
                    dto.setContent(n.getContent());
                    dto.setSeen(n.isSeen());
                    dto.setCreatedAt(n.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
