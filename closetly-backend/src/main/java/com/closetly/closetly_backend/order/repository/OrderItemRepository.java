package com.closetly.closetly_backend.order.repository;

import com.closetly.closetly_backend.order.entity.OrderItem;
import com.closetly.closetly_backend.order.entity.OrderItem.OrderItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * OrderItemRepository - Data access layer for OrderItem entities
 * 
 * OrderItems represent individual products in an order.
 * An Order can have multiple OrderItems (cart checkout pattern).
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Find all items in a specific order
     * 
     * @param orderId - The order ID
     * @return List of OrderItems in that order
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Find all items of a specific type (BUY or RENT) in an order
     * 
     * @param orderId - The order ID
     * @param type    - OrderItemType.BUY or OrderItemType.RENT
     * @return List of matching OrderItems
     */
    List<OrderItem> findByOrderIdAndType(Long orderId, OrderItemType type);

    /**
     * Find all order items for a specific product
     * Useful for tracking which orders contain a particular product
     * 
     * @param productId - The product ID
     * @return List of OrderItems containing that product
     */
    List<OrderItem> findByProductId(Long productId);

    /**
     * Find order items purchased by a customer (via order.customer)
     * Eagerly loads order and product to avoid N+1 queries
     * 
     * @param customerId - The customer ID
     * @return List of OrderItems belonging to that customer's orders
     */
    @Query("SELECT oi FROM OrderItem oi " +
            "LEFT JOIN FETCH oi.product " +
            "WHERE oi.order.customer.id = :customerId " +
            "ORDER BY oi.createdAt DESC")
    List<OrderItem> findByCustomerIdWithProductDetails(@Param("customerId") Long customerId);

    /**
     * Find all rental items for a customer
     * Useful for tracking active/past rentals
     * 
     * @param customerId - The customer ID
     * @param type       - OrderItemType.RENT
     * @return List of rental OrderItems for that customer
     */
    @Query("SELECT oi FROM OrderItem oi " +
            "WHERE oi.order.customer.id = :customerId " +
            "AND oi.type = :type " +
            "ORDER BY oi.createdAt DESC")
    List<OrderItem> findByCustomerIdAndType(@Param("customerId") Long customerId, @Param("type") OrderItemType type);
}