package com.closetly.closetly_backend.order.entity;

import com.closetly.closetly_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order Entity - Represents a customer order containing multiple items
 * 
 * Key Design Points:
 * - Does NOT contain product_id or price (handled by OrderItem)
 * - One Order can have multiple OrderItems (one-to-many relationship)
 * - totalAmount = sum of all OrderItem.totalPrice
 * - totalItems = sum of all OrderItem.quantity
 * - Cascade.ALL ensures OrderItems are persisted/deleted with Order
 */
@Data
@Builder
@Entity
@Table(name = "orders")
@Where(clause = "deleted = false")
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    /**
     * One-to-many relationship with OrderItem
     * - mappedBy: indicates OrderItem.order is the owning side
     * - cascade: ALL orders are deleted/updated when their items are
     * - fetch: LAZY to prevent N+1 queries
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private Integer totalItems;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PLACED;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private boolean deleted = false;

    public enum OrderStatus {
        PLACED,
        PAID,
        SHIPPED,
        DELIVERED,
        CANCELLED
    }
}
