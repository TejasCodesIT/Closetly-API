package com.closetly.closetly_backend.admin.entity;

import com.closetly.closetly_backend.review.entity.Review;
import com.closetly.closetly_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@Entity
@Table(name = "review_flags")
@NoArgsConstructor
@AllArgsConstructor
public class ReviewFlag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flagger_id", nullable = false)
    private User flagger;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlagStatus status = FlagStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime flaggedAt;

    public enum FlagStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
