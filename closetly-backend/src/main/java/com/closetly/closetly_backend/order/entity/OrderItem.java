package com.closetly.closetly_backend.order.entity;

import com.closetly.closetly_backend.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * OrderItem Entity - Represents a single item in an order
 * 
 * Key Design Points:
 * - ManyToOne relationship with Order (multiple items per order)
 * - ManyToOne relationship with Product
 * - Handles both BUY and RENT transactions
 * - Stores product-level data (unitPrice, quantity) at time of purchase
 * - For rentals: stores startDate, endDate, and calculated rentalDays
 */
@Data
@Builder
@Entity
@Table(name = "order_items")
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to parent Order
     * - ManyToOne: multiple items belong to one order
     * - nullable: false ensures every item has an order
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Reference to Product
     * - ManyToOne: multiple order items can reference the same product
     * - nullable: false ensures every item references a product
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity = 1;

    /**
     * Price per unit at time of purchase
     * - Stored to maintain historical pricing
     * - For BUY: sale price at purchase time
     * - For RENT: daily rental price at purchase time
     */
    @Column(nullable = false)
    private Double unitPrice;

    /**
     * Total price for this item (unitPrice * quantity * rentalDays if rental)
     * - For BUY: unitPrice * quantity
     * - For RENT: unitPrice * rentalDays * quantity
     */
    @Column(nullable = false)
    private Double totalPrice;

    /**
     * Type of transaction: BUY or RENT
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderItemType type;

    // ========== Rental-specific fields ==========
    /**
     * Start date for rental (null for BUY items)
     */
    private LocalDate startDate;

    /**
     * End date for rental (null for BUY items)
     */
    private LocalDate endDate;

    /**
     * Number of rental days (inclusive, null for BUY items)
     * Calculated as: ChronoUnit.DAYS.between(startDate, endDate) + 1
     */
    private Integer rentalDays;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum OrderItemType {
        BUY, RENT
    }
}