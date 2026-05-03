package com.closetly.closetly_backend.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ProductVariant Entity - Represents size-based variants of a product
 * Each variant has its own quantity/stock level
 */
@Data
@Builder
@Entity
@Table(name = "product_variants")
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String size;

    @Column(nullable = false)
    private Integer quantity = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Helper method to check if variant is available
    public boolean isAvailable() {
        return quantity != null && quantity > 0;
    }

    // Helper method to decrease quantity
    public void decreaseQuantity(int amount) {
        if (quantity == null || quantity < amount) {
            throw new IllegalStateException("Insufficient stock for size " + size);
        }
        quantity -= amount;
    }

    // Helper method to increase quantity
    public void increaseQuantity(int amount) {
        if (quantity == null) {
            quantity = 0;
        }
        quantity += amount;
    }
}