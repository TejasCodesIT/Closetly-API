package com.closetly.closetly_backend.order.service;

import com.closetly.closetly_backend.order.dto.CreateOrderRequestDTO;
import com.closetly.closetly_backend.order.dto.OrderDTO;
import com.closetly.closetly_backend.order.dto.OrderRequestDTO;
import com.closetly.closetly_backend.order.dto.OrderResponseDTO;

import java.util.List;

public interface OrderService {
    // Legacy method - kept for backward compatibility
    OrderResponseDTO createOrder(OrderRequestDTO request, String email);

    // New optimized method for creating orders with multiple items
    OrderDTO createOrder(CreateOrderRequestDTO request, String email);

    // New method: Create order from all cart items (e-commerce style checkout)
    OrderDTO placeOrder(String email);

    // Get orders for a customer
    List<OrderDTO> getCustomerOrders(String email);

    // Get order by ID with full details
    OrderDTO getOrderById(Long orderId, String email);

    // NEW: Get orders received by seller (for approval/rejection)
    List<OrderDTO> getSellerOrders(String email);

    // NEW: Approve order (seller only)
    OrderDTO approveOrder(Long orderId, String email);

    // NEW: Reject order (seller only)
    OrderDTO rejectOrder(Long orderId, String email);
}
