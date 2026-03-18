package com.closetly.closetly_backend.order.service;

import com.closetly.closetly_backend.order.dto.OrderRequestDTO;
import com.closetly.closetly_backend.order.dto.OrderResponseDTO;
import com.closetly.closetly_backend.order.entity.Order;
import com.closetly.closetly_backend.order.entity.Order.OrderStatus;
import com.closetly.closetly_backend.order.repository.OrderRepository;
import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.repository.ProductRepository;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO request, String email) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (!product.allowsBuy()) {
            throw new IllegalArgumentException("Product is not available for purchase");
        }

        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        if (product.getSeller().getId().equals(customer.getId())) {
            throw new IllegalStateException("You cannot order your own product");
        }

        Double price = firstNonNull(product.getBuyPrice(), product.getSalePrice());
        if (price == null) {
            throw new IllegalArgumentException("Product buy price is not set");
        }

        // Optional stock/quantity decrement for BUY products
        Integer qty = product.getQuantity();
        if (qty != null) {
            if (qty <= 0) {
                throw new IllegalStateException("Product is out of stock");
            }
            product.setQuantity(qty - 1);
        }

        Order order = Order.builder()
                .product(product)
                .customer(customer)
                .price(price)
                .status(OrderStatus.PLACED)
                .build();

        Order saved = orderRepository.save(order);
        return toDto(saved);
    }

    private OrderResponseDTO toDto(Order o) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(o.getId());
        dto.setProductId(o.getProduct().getId());
        dto.setCustomerId(o.getCustomer().getId());
        dto.setPrice(o.getPrice());
        dto.setStatus(o.getStatus().name());
        dto.setCreatedAt(o.getCreatedAt());
        return dto;
    }

    private static <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }
}

