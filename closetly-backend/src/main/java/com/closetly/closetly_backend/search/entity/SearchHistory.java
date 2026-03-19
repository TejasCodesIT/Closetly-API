package com.closetly.closetly_backend.search.entity;

import com.closetly.closetly_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@Entity
@Table(name = "search_history")
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 500, nullable = false)
    private String query;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

