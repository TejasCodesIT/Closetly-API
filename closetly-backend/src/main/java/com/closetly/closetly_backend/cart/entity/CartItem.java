package com.closetly.closetly_backend.cart.entity;

import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.user.entity.User;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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
@Table(name = "cart_items")
@Where(clause = "deleted = false")
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    @Id
    @Column(length = 36)
    private String id; // UUID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartItemType type;

    @Column(nullable = false)
    private Integer quantity = 1;

    private LocalDate startDate; // for rentals only
    private LocalDate endDate; // for rentals only

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private boolean deleted = false;

    public enum CartItemType {
        RENT, BUY;

        @JsonCreator
        public static CartItemType fromString(String value) {
            return CartItemType.valueOf(value.toUpperCase());
        }

        @JsonValue
        public String toValue() {
            return this.name().toLowerCase();
        }
    }

    // Helper method to calculate rental days
    public long getRentalDays() {
        if (startDate != null && endDate != null) {
            return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1; // inclusive
        }
        return 0;
    }

    // Helper method to calculate item total
    public double getItemTotal() {
        if (type == CartItemType.RENT && product.getRentPricePerDay() != null) {
            return product.getRentPricePerDay() * getRentalDays() * quantity;
        } else if (type == CartItemType.BUY && product.getSalePrice() != null) {
            return product.getSalePrice() * quantity;
        }
        return 0.0;
    }
}