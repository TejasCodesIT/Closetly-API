package com.closetly.closetly_backend.order.controller;

import com.closetly.closetly_backend.order.dto.CreateOrderRequestDTO;
import com.closetly.closetly_backend.order.dto.OrderDTO;
import com.closetly.closetly_backend.order.dto.OrderRequestDTO;
import com.closetly.closetly_backend.order.dto.OrderResponseDTO;
import com.closetly.closetly_backend.order.dto.SellerOrderItemDTO;
import com.closetly.closetly_backend.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // Legacy endpoint for backward compatibility
    @PostMapping("/single")
    public ResponseEntity<OrderResponseDTO> createSingleOrder(@Valid @RequestBody OrderRequestDTO request,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.createOrder(request, email));
    }

    // New optimized endpoint for creating orders with multiple items
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody CreateOrderRequestDTO request,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.createOrder(request, email));
    }

    // E-commerce style checkout: Create order from all cart items
    @PostMapping("/checkout")
    public ResponseEntity<List<OrderDTO>> placeOrder(Authentication authentication) {
        String email = authentication.getName();
        List<OrderDTO> orders = orderService.placeOrder(email);
        return ResponseEntity.ok(orders);
    }

    // Get customer's orders
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getCustomerOrders(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.getCustomerOrders(email));
    }

    // New pagination-enabled endpoint for customer orders
    @GetMapping("/my-orders")
    public ResponseEntity<Page<OrderDTO>> getMyOrders(Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.getCustomerOrders(email, page, size));
    }

    // Get pending order requests for seller
    @GetMapping("/order-requests")
    public ResponseEntity<Page<OrderDTO>> getOrderRequests(Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.getOrderRequests(email, page, size));
    }

    // Get order history (customer or seller)
    @GetMapping("/order-history")
    public ResponseEntity<Page<OrderDTO>> getOrderHistory(Authentication authentication,
            @RequestParam(defaultValue = "false") boolean seller,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.getOrderHistory(email, seller, page, size));
    }

    // Get specific order details
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long orderId, Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.getOrderById(orderId, email));
    }

    // ========== NEW ENDPOINTS FOR BUY FLOW ==========

    // Get orders received by seller (for approval/rejection)
    @GetMapping("/seller")
    public ResponseEntity<List<OrderDTO>> getSellerOrders(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.getSellerOrders(email));
    }

    // Paginated seller orders endpoint
    @GetMapping("/seller/order-items")
    public ResponseEntity<Page<SellerOrderItemDTO>> getSellerOrderItems(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String email = authentication.getName();
        return ResponseEntity.ok(orderService.getSellerOrderItems(email, page, size));
    }

    // Approve order (seller only)
    @PostMapping("/{orderId}/approve")
    public ResponseEntity<OrderDTO> approveOrder(@PathVariable Long orderId, Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.approveOrder(orderId, email));
    }

    // Reject order (seller only)
    @PostMapping("/{orderId}/reject")
    public ResponseEntity<OrderDTO> rejectOrder(@PathVariable Long orderId, Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.rejectOrder(orderId, email));
    }

    // ========== NEW SELLER-CENTRIC ENDPOINTS ==========

    @GetMapping("/order-items/seller")
    public ResponseEntity<Page<SellerOrderItemDTO>> getSellerOrderItemsALT(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String email = authentication.getName();
        return ResponseEntity.ok(orderService.getSellerOrderItems(email, page, size));
    }

    // Approve order item (seller only)
    @PostMapping("/order-items/{orderItemId}/approve")
    public ResponseEntity<SellerOrderItemDTO> approveOrderItem(@PathVariable Long orderItemId,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.approveOrderItem(orderItemId, email));
    }

    // Reject order item (seller only)
    @PostMapping("/order-items/{orderItemId}/reject")
    public ResponseEntity<SellerOrderItemDTO> rejectOrderItem(@PathVariable Long orderItemId,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.rejectOrderItem(orderItemId, email));
    }
}
