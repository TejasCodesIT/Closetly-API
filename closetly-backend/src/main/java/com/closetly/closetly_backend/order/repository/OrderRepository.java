package com.closetly.closetly_backend.order.repository;

import com.closetly.closetly_backend.order.entity.Order;
import com.closetly.closetly_backend.order.entity.Order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * OrderRepository - Data access layer for Order entities
 * 
 * Optimized queries to prevent N+1 problems by eagerly loading:
 * - OrderItems (using LEFT JOIN FETCH)
 * - Products (via OrderItems)
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

        /**
         * Find order by ID with all related items and products in ONE query
         * Uses LEFT JOIN FETCH to eagerly load the entire order graph
         */
        @Query("SELECT o FROM Order o " +
                        "LEFT JOIN FETCH o.orderItems oi " +
                        "LEFT JOIN FETCH oi.product " +
                        "WHERE o.id = :orderId")
        @EntityGraph(attributePaths = { "orderItems.product.images" })
        Optional<Order> findByIdWithItemsAndProducts(@Param("orderId") Long orderId);

        /**
         * Find all orders for a customer with their items and products
         * Ordered by creation date (newest first)
         * Uses LEFT JOIN FETCH to prevent N+1 queries
         */
        @Query("SELECT o FROM Order o " +
                        "LEFT JOIN FETCH o.orderItems oi " +
                        "LEFT JOIN FETCH oi.product " +
                        "WHERE o.customer.id = :customerId " +
                        "ORDER BY o.createdAt DESC")
        @EntityGraph(attributePaths = { "orderItems.product.images" })
        List<Order> findByCustomerIdWithItemsAndProducts(@Param("customerId") Long customerId);

        /**
         * Find the most recent order containing a specific product for a customer with
         * given statuses
         * This joins through OrderItem to filter by product while maintaining order
         * context
         * 
         * @param productId  - Product ID to search for
         * @param customerId - Customer ID who owns the order
         * @param statuses   - Collection of order statuses to filter by (e.g.,
         *                   DELIVERED, PAID)
         * @return Optional containing the most recent matching order
         */
        @Query("SELECT DISTINCT o FROM Order o " +
                        "JOIN o.orderItems oi " +
                        "WHERE oi.product.id = :productId " +
                        "AND o.customer.id = :customerId " +
                        "AND o.status IN :statuses " +
                        "ORDER BY o.createdAt DESC")
        @EntityGraph(attributePaths = { "orderItems.product.images" })
        Optional<Order> findLatestOrderByProductIdAndCustomerIdAndStatusIn(
                        @Param("productId") Long productId,
                        @Param("customerId") Long customerId,
                        @Param("statuses") Collection<OrderStatus> statuses);

        /**
         * Find all orders for a seller with their items and products
         * Ordered by creation date (newest first)
         * Uses LEFT JOIN FETCH to prevent N+1 queries
         */
        @Query("SELECT o FROM Order o " +
                        "LEFT JOIN FETCH o.orderItems oi " +
                        "LEFT JOIN FETCH oi.product " +
                        "WHERE o.seller.id = :sellerId " +
                        "ORDER BY o.createdAt DESC")
        @EntityGraph(attributePaths = { "orderItems.product.images" })
        List<Order> findBySellerIdWithItemsAndProducts(@Param("sellerId") Long sellerId);

        Page<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

        Page<Order> findByCustomerIdAndStatusOrderByCreatedAtDesc(Long customerId, OrderStatus status,
                        Pageable pageable);

        Page<Order> findByCustomerIdAndStatusInOrderByCreatedAtDesc(Long customerId, Collection<OrderStatus> statuses,
                        Pageable pageable);

        Page<Order> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

        Page<Order> findBySellerIdAndStatusInOrderByCreatedAtDesc(Long sellerId, Collection<OrderStatus> statuses,
                        Pageable pageable);

        /**
         * Find all orders with a specific status
         * Useful for order management (e.g., finding all shipped orders)
         */
        List<Order> findByStatus(OrderStatus status);

        /**
         * Count orders for a specific customer
         */
        Integer countByCustomerId(Long customerId);

        /**
         * Find orders containing a specific product with given status (excluding a
         * specific order)
         */
        @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi WHERE oi.product.id = :productId AND o.status = :status AND o.id != :excludeOrderId")
        List<Order> findByProductIdAndStatusAndIdNot(@Param("productId") Long productId,
                        @Param("status") OrderStatus status, @Param("excludeOrderId") Long excludeOrderId);
}
