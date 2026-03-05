package com.closetly.closetly_backend.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@Entity
@Table(name = "system_logs", indexes = {
        @Index(name = "idx_created_at", columnList = "created_at DESC"),
        @Index(name = "idx_type", columnList = "type")
})
@NoArgsConstructor
@AllArgsConstructor
public class SystemLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogType type;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(length = 5000)
    private String details;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum LogType {
        REPORT_SUBMITTED,
        REPORT_APPROVED,
        REPORT_REJECTED,
        PRODUCT_BLOCKED,
        USER_BANNED,
        REVIEW_DELETED,
        SYSTEM_ACTION
    }
}
