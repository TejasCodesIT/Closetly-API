package com.closetly.closetly_backend.notification.service;

import com.closetly.closetly_backend.notification.dto.NotificationDTO;
import com.closetly.closetly_backend.notification.entity.Notification;
import com.closetly.closetly_backend.notification.repository.NotificationRepository;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void notifyUserByEmail(String email, String message) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .read(false)
                .build();

        notificationRepository.save(Objects.requireNonNull(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getMyNotifications(String email) {
        return notificationRepository.findByUserEmailOrderByCreatedAtDesc(email).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private NotificationDTO toDto(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(n.getId());
        dto.setUserId(n.getUser().getId());
        dto.setContent(n.getMessage());
        dto.setSeen(n.isRead());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }
}
