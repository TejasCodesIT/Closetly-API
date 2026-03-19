package com.closetly.closetly_backend.notification.repository;

import com.closetly.closetly_backend.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserEmailOrderByCreatedAtDesc(String email);
}
