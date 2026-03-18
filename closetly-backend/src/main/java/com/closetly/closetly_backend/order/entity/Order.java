package com.closetly.closetly_backend.order.entity;

import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

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
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(nullable = false)
    private Double price;

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

