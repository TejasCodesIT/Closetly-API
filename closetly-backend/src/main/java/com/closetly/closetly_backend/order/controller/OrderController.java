package com.closetly.closetly_backend.order.controller;

import com.closetly.closetly_backend.order.dto.CreateOrderRequestDTO;
import com.closetly.closetly_backend.order.dto.OrderDTO;
import com.closetly.closetly_backend.order.dto.OrderRequestDTO;
import com.closetly.closetly_backend.order.dto.OrderResponseDTO;
import com.closetly.closetly_backend.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<OrderDTO> placeOrder(Authentication authentication) {
        String email = authentication.getName();
        OrderDTO order = orderService.placeOrder(email);
        return ResponseEntity.ok(order);
    }

    // Get customer's orders
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getCustomerOrders(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.getCustomerOrders(email));
    }

    // Get specific order details
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long orderId, Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.getOrderById(orderId, email));
    }
}
