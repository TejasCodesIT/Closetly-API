package com.closetly.closetly_backend.order.service;

import com.closetly.closetly_backend.order.dto.CreateOrderRequestDTO;
import com.closetly.closetly_backend.order.dto.OrderDTO;
import com.closetly.closetly_backend.order.dto.OrderRequestDTO;
import com.closetly.closetly_backend.order.dto.OrderResponseDTO;
import com.closetly.closetly_backend.order.dto.SellerOrderItemDTO;

import java.util.List;

import org.springframework.data.domain.Page;

public interface OrderService {
    // Legacy method - kept for backward compatibility
    OrderResponseDTO createOrder(OrderRequestDTO request, String email);

    // New optimized method for creating orders with multiple items
    OrderDTO createOrder(CreateOrderRequestDTO request, String email);

    // New method: Create order from all cart items (e-commerce style checkout)
    List<OrderDTO> placeOrder(String email);

    // Get orders for a customer
    List<OrderDTO> getCustomerOrders(String email);

    // Get customer orders with pagination
    org.springframework.data.domain.Page<OrderDTO> getCustomerOrders(String email, int page, int size);

    org.springframework.data.domain.Page<OrderDTO> getCustomerOrders(String email, int page, int size, String status);

    // Get order history for customer/seller with pagination
    org.springframework.data.domain.Page<OrderDTO> getOrderHistory(String email, boolean sellerSide, int page,
            int size);

    // Get seller order requests with pagination
    org.springframework.data.domain.Page<OrderDTO> getOrderRequests(String email, int page, int size);

    // Get order by ID with full details
    OrderDTO getOrderById(Long orderId, String email);

    // NEW: Get orders received by seller (for approval/rejection)
    List<OrderDTO> getSellerOrders(String email);

    // NEW: Get orders received by seller (for approval/rejection) with pagination
    org.springframework.data.domain.Page<OrderDTO> getSellerOrders(String email, int page, int size);

    // NEW: Approve order (seller only)
    OrderDTO approveOrder(Long orderId, String email);

    // NEW: Reject order (seller only)
    OrderDTO rejectOrder(Long orderId, String email);

    // NEW: Get order items for seller (item-centric view)
    List<SellerOrderItemDTO> getSellerOrderItems(String email);

    // NEW: Approve order item (seller only)
    SellerOrderItemDTO approveOrderItem(Long orderItemId, String email);

    // NEW: Reject order item (seller only)
    SellerOrderItemDTO rejectOrderItem(Long orderItemId, String email);

    Page<SellerOrderItemDTO> getSellerOrderItems(String email, int page, int size);

    // NEW: Cancel order (customer or seller)
    OrderDTO cancelOrder(Long orderId, String email, String reason);

    // NEW: Cancel order item (seller only)
    SellerOrderItemDTO cancelOrderItem(Long orderItemId, String email, String reason);
}
