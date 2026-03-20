package com.closetly.closetly_backend.order.repository;

import com.closetly.closetly_backend.order.entity.Order;
import com.closetly.closetly_backend.order.entity.Order.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findFirstByProductIdAndCustomerIdAndStatusIn(Long productId, Long customerId,
            Collection<OrderStatus> statuses);
}

