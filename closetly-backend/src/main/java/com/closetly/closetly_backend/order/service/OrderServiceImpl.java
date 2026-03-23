package com.closetly.closetly_backend.order.service;

import com.closetly.closetly_backend.order.dto.*;
import com.closetly.closetly_backend.order.entity.Order;
import com.closetly.closetly_backend.order.entity.Order.OrderStatus;
import com.closetly.closetly_backend.order.entity.OrderItem;
import com.closetly.closetly_backend.order.entity.OrderItem.OrderItemType;
import com.closetly.closetly_backend.order.repository.OrderItemRepository;
import com.closetly.closetly_backend.order.repository.OrderRepository;
import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.repository.ProductRepository;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import com.closetly.closetly_backend.cart.entity.CartItem;
import com.closetly.closetly_backend.cart.repository.CartItemRepository;
import com.closetly.closetly_backend.cart.entity.CartItem.CartItemType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO request, String email) {
        // Legacy method - creates single item order
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

        OrderItem orderItem = OrderItem.builder()
                .product(product)
                .quantity(1)
                .unitPrice(price)
                .totalPrice(price)
                .type(OrderItemType.BUY)
                .build();

        Order order = Order.builder()
                .customer(customer)
                .orderItems(List.of(orderItem))
                .totalAmount(price)
                .totalItems(1)
                .status(OrderStatus.PLACED)
                .build();

        orderItem.setOrder(order);

        Order saved = orderRepository.save(order);
        return toLegacyDto(saved);
    }

    @Override
    @Transactional
    public OrderDTO createOrder(CreateOrderRequestDTO request, String email) {
        // Validate customer
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        // Get all product IDs and fetch them in a single query
        List<Long> productIds = request.getItems().stream()
                .map(OrderItemRequestDTO::getProductId)
                .distinct()
                .toList();

        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // Validate all products exist and are available
        for (OrderItemRequestDTO itemRequest : request.getItems()) {
            Product product = productMap.get(itemRequest.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("Product not found: " + itemRequest.getProductId());
            }

            // Check ownership
            if (product.getSeller().getId().equals(customer.getId())) {
                throw new IllegalStateException("You cannot order your own product: " + product.getTitle());
            }

            // Validate based on type
            OrderItemType itemType = OrderItemType.valueOf(itemRequest.getType().toUpperCase());
            if (itemType == OrderItemType.RENT && !product.allowsRent()) {
                throw new IllegalArgumentException("Product is not available for rent: " + product.getTitle());
            }
            if (itemType == OrderItemType.BUY && !product.allowsBuy()) {
                throw new IllegalArgumentException("Product is not available for purchase: " + product.getTitle());
            }

            // Validate rental dates
            if (itemType == OrderItemType.RENT) {
                validateRentalDates(itemRequest.getStartDate(), itemRequest.getEndDate());
            }
        }

        // Create order items
        List<OrderItem> orderItems = request.getItems().stream()
                .map(itemRequest -> {
                    Product product = productMap.get(itemRequest.getProductId());
                    OrderItemType itemType = OrderItemType.valueOf(itemRequest.getType().toUpperCase());

                    double unitPrice = calculateUnitPrice(product, itemType, itemRequest.getStartDate(),
                            itemRequest.getEndDate());
                    double totalPrice = unitPrice * itemRequest.getQuantity();

                    Integer rentalDays = null;
                    if (itemType == OrderItemType.RENT) {
                        rentalDays = (int) ChronoUnit.DAYS.between(itemRequest.getStartDate(), itemRequest.getEndDate())
                                + 1;
                    }

                    return OrderItem.builder()
                            .product(product)
                            .quantity(itemRequest.getQuantity())
                            .unitPrice(unitPrice)
                            .totalPrice(totalPrice)
                            .type(itemType)
                            .startDate(itemRequest.getStartDate())
                            .endDate(itemRequest.getEndDate())
                            .rentalDays(rentalDays)
                            .build();
                })
                .toList();

        // Calculate order totals
        double totalAmount = orderItems.stream().mapToDouble(OrderItem::getTotalPrice).sum();
        int totalItems = orderItems.stream().mapToInt(OrderItem::getQuantity).sum();

        // Create order
        Order order = Order.builder()
                .customer(customer)
                .orderItems(orderItems)
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .status(OrderStatus.PLACED)
                .build();

        // Set bidirectional relationship
        orderItems.forEach(item -> item.setOrder(order));

        // Update product quantities for BUY items
        for (OrderItem item : orderItems) {
            if (item.getType() == OrderItemType.BUY) {
                Product product = item.getProduct();
                Integer currentQty = product.getQuantity();
                if (currentQty != null && currentQty > 0) {
                    product.setQuantity(currentQty - item.getQuantity());
                }
            }
        }

        Order saved = orderRepository.save(order);
        return toDto(saved);
    }

    @Override
    @Transactional
    public OrderDTO placeOrder(String email) {
        // 1. Validate and get customer
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        // 2. Fetch all cart items for the user (with products already joined to avoid
        // N+1)
        List<CartItem> cartItems = cartItemRepository.findByUserIdWithProducts(customer.getId());

        // 3. Validate cart is not empty
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty. Add items to cart before placing order.");
        }

        // 4. Extract product IDs and fetch all products in a single batch query
        // (prevents N+1 queries)
        List<Long> productIds = cartItems.stream()
                .map(cartItem -> cartItem.getProduct().getId())
                .distinct()
                .toList();

        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 5. Calculate total amount and validate cart items
        double totalAmount = 0.0;
        int totalItems = 0;

        // Validate all cart items and calculate totals
        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProduct().getId());
            if (product == null) {
                throw new IllegalArgumentException("Product not found: " + cartItem.getProduct().getId());
            }

            // Check ownership - can't order own products
            if (product.getSeller().getId().equals(customer.getId())) {
                throw new IllegalStateException("You cannot order your own product: " + product.getTitle());
            }

            // Calculate item total based on type
            double itemTotal;
            if (cartItem.getType() == CartItemType.BUY) {
                // Validate product is available for purchase
                if (!product.allowsBuy()) {
                    throw new IllegalArgumentException("Product is not available for purchase: " + product.getTitle());
                }
                // Use salePrice for buying
                Double salePrice = product.getSalePrice();
                if (salePrice == null || salePrice <= 0) {
                    throw new IllegalArgumentException("Invalid sale price for product: " + product.getTitle());
                }
                itemTotal = salePrice * cartItem.getQuantity();

            } else if (cartItem.getType() == CartItemType.RENT) {
                // Validate product is available for rent
                if (!product.allowsRent()) {
                    throw new IllegalArgumentException("Product is not available for rent: " + product.getTitle());
                }
                // Validate rental dates
                if (cartItem.getStartDate() == null || cartItem.getEndDate() == null) {
                    throw new IllegalArgumentException("Rental dates are required for rental items");
                }
                validateRentalDates(cartItem.getStartDate(), cartItem.getEndDate());

                // Use rentPricePerDay for renting
                Double rentPricePerDay = product.getRentPricePerDay();
                if (rentPricePerDay == null || rentPricePerDay <= 0) {
                    throw new IllegalArgumentException("Invalid rental price for product: " + product.getTitle());
                }

                // Calculate rental days (inclusive)
                long rentalDays = java.time.temporal.ChronoUnit.DAYS.between(cartItem.getStartDate(),
                        cartItem.getEndDate()) + 1;
                itemTotal = rentPricePerDay * rentalDays * cartItem.getQuantity();

            } else {
                throw new IllegalArgumentException("Invalid cart item type: " + cartItem.getType());
            }

            totalAmount += itemTotal;
            totalItems += cartItem.getQuantity();
        }

        // 6. Create OrderItems from CartItems
        List<OrderItem> orderItems = cartItems.stream()
                .map(cartItem -> {
                    Product product = productMap.get(cartItem.getProduct().getId());

                    // Recalculate unit price and total for this order item
                    double unitPrice;
                    Integer rentalDays = null;

                    if (cartItem.getType() == CartItemType.BUY) {
                        unitPrice = product.getSalePrice();
                    } else {
                        unitPrice = product.getRentPricePerDay();
                        rentalDays = (int) (java.time.temporal.ChronoUnit.DAYS.between(cartItem.getStartDate(),
                                cartItem.getEndDate()) + 1);
                    }

                    double totalPrice = unitPrice * cartItem.getQuantity();
                    if (cartItem.getType() == CartItemType.RENT) {
                        totalPrice = unitPrice * rentalDays * cartItem.getQuantity();
                    }

                    return OrderItem.builder()
                            .product(product)
                            .quantity(cartItem.getQuantity())
                            .unitPrice(unitPrice)
                            .totalPrice(totalPrice)
                            .type(cartItem.getType() == CartItemType.BUY ? OrderItemType.BUY : OrderItemType.RENT)
                            .startDate(cartItem.getStartDate())
                            .endDate(cartItem.getEndDate())
                            .rentalDays(rentalDays)
                            .build();
                })
                .toList();

        // 7. Create the Order with calculated totals
        Order order = Order.builder()
                .customer(customer)
                .orderItems(orderItems)
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .status(OrderStatus.PLACED)
                .build();

        // Set bidirectional relationship
        orderItems.forEach(item -> item.setOrder(order));

        // 8. Update product quantities for BUY items (decrement stock)
        for (OrderItem item : orderItems) {
            if (item.getType() == OrderItemType.BUY) {
                Product product = item.getProduct();
                Integer currentQty = product.getQuantity();
                if (currentQty != null && currentQty >= item.getQuantity()) {
                    product.setQuantity(currentQty - item.getQuantity());
                } else {
                    throw new IllegalStateException("Insufficient stock for product: " + product.getTitle());
                }
            }
        }

        // 9. Save the order (cascade will save order items)
        Order savedOrder = orderRepository.save(order);

        // 10. Clear the cart after successful order creation
        cartItemRepository.deleteByUserId(customer.getId());

        return toDto(savedOrder);
    }

    @Override
    public List<OrderDTO> getCustomerOrders(String email) {
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        List<Order> orders = orderRepository.findByCustomerIdWithItemsAndProducts(customer.getId());
        return orders.stream().map(this::toDto).toList();
    }

    @Override
    public OrderDTO getOrderById(Long orderId, String email) {
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        Order order = orderRepository.findByIdWithItemsAndProducts(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalStateException("You can only view your own orders");
        }

        return toDto(order);
    }

    private double calculateUnitPrice(Product product, OrderItemType type, LocalDate startDate, LocalDate endDate) {
        if (type == OrderItemType.RENT) {
            if (product.getRentPricePerDay() == null) {
                throw new IllegalArgumentException("Rental price not set for product: " + product.getTitle());
            }
            return product.getRentPricePerDay();
        } else {
            Double price = firstNonNull(product.getBuyPrice(), product.getSalePrice());
            if (price == null) {
                throw new IllegalArgumentException("Buy price not set for product: " + product.getTitle());
            }
            return price;
        }
    }

    private void validateRentalDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required for rentals");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
    }

    private OrderDTO toDto(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setCustomerId(order.getCustomer().getId());
        dto.setCustomerName(order.getCustomer().getFullName());
        dto.setCustomerEmail(order.getCustomer().getEmail());
        dto.setOrderItems(order.getOrderItems().stream().map(this::toOrderItemDto).toList());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setTotalItems(order.getTotalItems());
        dto.setStatus(order.getStatus().name());
        dto.setCreatedAt(order.getCreatedAt());
        return dto;
    }

    private OrderItemDTO toOrderItemDto(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(item.getId());
        Product product = item.getProduct();
        dto.setProductId(product.getId());
        dto.setProductTitle(product.getTitle());
        dto.setProductBrand(product.getBrand());
        dto.setProductImage(
                product.getImages() != null && !product.getImages().isEmpty() ? product.getImages().get(0) : null);
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        dto.setType(item.getType().name());
        dto.setStartDate(item.getStartDate());
        dto.setEndDate(item.getEndDate());
        dto.setRentalDays(item.getRentalDays());
        dto.setCreatedAt(item.getCreatedAt());
        return dto;
    }

    private OrderResponseDTO toLegacyDto(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());

        // Get product details from first order item (legacy single-item orders)
        if (!order.getOrderItems().isEmpty()) {
            OrderItem firstItem = order.getOrderItems().get(0);
            Product product = firstItem.getProduct();

            dto.setProductId(product.getId());
            dto.setProductTitle(product.getTitle());
            dto.setProductBrand(product.getBrand());

            // Get first image URL safely
            dto.setProductImageUrl(
                    product.getImages() != null && !product.getImages().isEmpty()
                            ? product.getImages().get(0)
                            : null);

            dto.setPrice(firstItem.getTotalPrice());
        }

        dto.setCustomerId(order.getCustomer().getId());
        dto.setStatus(order.getStatus().name());
        dto.setCreatedAt(order.getCreatedAt());
        return dto;
    }

    private static <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }
}
