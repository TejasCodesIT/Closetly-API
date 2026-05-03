package com.closetly.closetly_backend.booking.entity;

import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@Entity
@Table(name = "bookings")
@Where(clause = "deleted = false")
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private BookingStatus status = BookingStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime cancelledAt;

    @Column(length = 500)
    private String cancelReason;

    @Enumerated(EnumType.STRING)
    private CancelledBy cancelledBy;

    private boolean deleted = false;

    public enum BookingStatus {
        PENDING,
        ACCEPTED,
        REJECTED,
        CANCELLED_BY_CUSTOMER,
        CANCELLED_BY_SELLER,
        DELIVERED,
        COMPLETED, APPROVED, CANCELLED
    }

    public enum CancelledBy {
        CUSTOMER, SELLER
    }
}
