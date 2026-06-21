package com.closetly.closetly_backend.order.service;

import com.closetly.closetly_backend.order.dto.*;
import com.closetly.closetly_backend.order.entity.Order;
import com.closetly.closetly_backend.order.entity.Order.OrderStatus;
import com.closetly.closetly_backend.order.entity.OrderItem;
import com.closetly.closetly_backend.order.entity.OrderItem.OrderItemType;
import com.closetly.closetly_backend.chat.entity.ChatRoom;
import com.closetly.closetly_backend.chat.repository.ChatRoomRepository;
import com.closetly.closetly_backend.order.repository.OrderItemRepository;
import com.closetly.closetly_backend.order.repository.OrderRepository;
import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.entity.ProductVariant;
import com.closetly.closetly_backend.product.repository.ProductRepository;
import com.closetly.closetly_backend.product.repository.ProductVariantRepository;
import com.closetly.closetly_backend.user.service.EmailService;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import com.closetly.closetly_backend.cart.entity.CartItem;
import com.closetly.closetly_backend.cart.repository.CartItemRepository;
import com.closetly.closetly_backend.cart.entity.CartItem.CartItemType;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final EmailService emailService;

    /**
     * Validates and sets default status for orders
     */
    private void validateAndSetOrderStatus(Order order) {
        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.PLACED);
        }
    }

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
                .seller(product.getSeller())
                .orderItems(List.of(orderItem))
                .totalAmount(price)
                .totalItems(1)
                .status(OrderStatus.PLACED)
                .build();

        orderItem.setOrder(order);

        // Validate and set status with debug logging
        validateAndSetOrderStatus(order);

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

                    // Handle size-based variants
                    ProductVariant variant = null;
                    String size = itemRequest.getSize();
                    if (size != null && !size.trim().isEmpty()) {
                        variant = productVariantRepository.findByProductAndSize(product, size.trim())
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Product variant not found for size: " + size + " in product: "
                                                + product.getTitle()));
                    }

                    return OrderItem.builder()
                            .product(product)
                            .productVariant(variant)
                            .size(size)
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
                .seller(orderItems.get(0).getProduct().getSeller()) // Assume all items from same seller
                .orderItems(orderItems)
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .status(OrderStatus.PLACED)
                .build();

        // Set bidirectional relationship
        orderItems.forEach(item -> item.setOrder(order));

        // Validate and set status with debug logging
        validateAndSetOrderStatus(order);

        // Update product quantities for BUY items
        for (OrderItem item : orderItems) {
            if (item.getType() == OrderItemType.BUY) {
                if (item.getProductVariant() != null) {
                    // Update variant quantity
                    item.getProductVariant().decreaseQuantity(item.getQuantity());
                } else {
                    // Fallback to product quantity (for backward compatibility)
                    Product product = item.getProduct();
                    Integer currentQty = product.getQuantity();
                    if (currentQty != null && currentQty > 0) {
                        product.setQuantity(currentQty - item.getQuantity());
                    }
                }
            }
        }

        Order saved = orderRepository.save(order);

        return toDto(saved);
    }

    @Override
    @Transactional
    public List<OrderDTO> placeOrder(String email) {
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

        // no-op placeholder to keep the stream semantics explicit
        cartItems.forEach(item -> {
        });

        // 4. Group cart items by seller
        Map<Long, List<CartItem>> itemsBySeller = cartItems.stream()
                .collect(Collectors.groupingBy(item -> {
                    Long sellerId = item.getProduct().getSeller().getId();
                    return sellerId;
                }));
        // 5. Extract product IDs and fetch all products in a single batch query
        List<Long> productIds = cartItems.stream()
                .map(cartItem -> cartItem.getProduct().getId())
                .distinct()
                .toList();

        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 6. Create separate orders for each seller
        List<Order> orders = new ArrayList<>();

        for (Map.Entry<Long, List<CartItem>> entry : itemsBySeller.entrySet()) {
            List<CartItem> sellerCartItems = entry.getValue();

            // Get seller
            User seller = sellerCartItems.get(0).getProduct().getSeller();

            // Calculate totals for this seller's items
            double sellerTotalAmount = 0.0;
            int sellerTotalItems = 0;
            List<OrderItem> orderItems = new ArrayList<>();

            // Validate and create order items for this seller
            for (CartItem cartItem : sellerCartItems) {
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
                Integer rentalDays = null;

                if (cartItem.getType() == CartItemType.BUY) {
                    // Validate product is available for purchase
                    if (!product.allowsBuy()) {
                        throw new IllegalArgumentException(
                                "Product is not available for purchase: " + product.getTitle());
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

                    // Use rentPrice for renting
                    Double rentPrice = product.getRentPrice();
                    if (rentPrice == null || rentPrice <= 0) {
                        throw new IllegalArgumentException("Invalid rental price for product: " + product.getTitle());
                    }

                    // Calculate rental days (inclusive)
                    rentalDays = (int) (ChronoUnit.DAYS.between(cartItem.getStartDate(), cartItem.getEndDate()) + 1);
                    itemTotal = rentPrice * rentalDays * cartItem.getQuantity();
                } else {
                    throw new IllegalArgumentException("Invalid cart item type: " + cartItem.getType());
                }

                sellerTotalAmount += itemTotal;
                sellerTotalItems += cartItem.getQuantity();

                // Create OrderItem
                OrderItem orderItem = OrderItem.builder()
                        .product(product)
                        .quantity(cartItem.getQuantity())
                        .unitPrice(cartItem.getType() == CartItemType.BUY ? product.getSalePrice()
                                : product.getRentPrice())
                        .totalPrice(itemTotal)
                        .type(cartItem.getType() == CartItemType.BUY ? OrderItemType.BUY : OrderItemType.RENT)
                        .startDate(cartItem.getStartDate())
                        .endDate(cartItem.getEndDate())
                        .rentalDays(rentalDays)
                        .build();

                orderItems.add(orderItem);
            }

            // Create the Order for this seller
            Order order = Order.builder()
                    .customer(customer)
                    .seller(seller)
                    .orderItems(orderItems)
                    .totalAmount(sellerTotalAmount)
                    .totalItems(sellerTotalItems)
                    .status(OrderStatus.PLACED)
                    .build();

            // Set bidirectional relationship
            orderItems.forEach(item -> item.setOrder(order));

            // Validate and set status with debug logging
            validateAndSetOrderStatus(order);

            // TODO: Stock will be decreased when order is approved by seller
            // Update product quantities for BUY items (decrement stock)
            // for (OrderItem item : orderItems) {
            // if (item.getType() == OrderItemType.BUY) {
            // Product product = item.getProduct();
            // Integer currentQty = product.getQuantity();
            // if (currentQty != null && currentQty >= item.getQuantity()) {
            // product.setQuantity(currentQty - item.getQuantity());
            // } else {
            // throw new IllegalStateException("Insufficient stock for product: " +
            // product.getTitle());
            // }
            // }
            // }

            // Save the order (cascade will save order items)
            Order savedOrder = orderRepository.save(order);

            orders.add(savedOrder);
        }

        // 7. Clear the cart after successful order creation
        cartItemRepository.deleteByUserId(customer.getId());

        // 8. Return list of order DTOs
        return orders.stream().map(this::toDto).toList();
    }

    @Override
    public List<OrderDTO> getCustomerOrders(String email) {
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        List<Order> orders = orderRepository.findByCustomerIdWithItemsAndProducts(customer.getId());
        return orders.stream().map(this::toDto).toList();
    }

    @Override
    public Page<OrderDTO> getCustomerOrders(String email, int page, int size) {
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<Order> ordersPage = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId(), pageable);
        return ordersPage.map(this::toDto);
    }

    @Override
    public Page<OrderDTO> getCustomerOrders(String email, int page, int size, String status) {
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));

        Page<Order> ordersPage;
        if (status != null && !status.isEmpty()) {
            // Handle CANCELLED status to include all cancelled types
            if ("CANCELLED".equals(status)) {
                ordersPage = orderRepository.findByCustomerIdAndStatusInOrderByCreatedAtDesc(
                        customer.getId(),
                        List.of(OrderStatus.CANCELLED_BY_CUSTOMER, OrderStatus.CANCELLED_BY_SELLER,
                                OrderStatus.CANCELLED),
                        pageable);
            } else {
                OrderStatus orderStatus = OrderStatus.valueOf(status);
                ordersPage = orderRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(customer.getId(),
                        orderStatus, pageable);
            }
        } else {
            ordersPage = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId(), pageable);
        }

        return ordersPage.map(this::toDto);
    }

    @Override
    public Page<SellerOrderItemDTO> getSellerOrderItems(String email, int page, int size) {

        User seller = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found"));

        Pageable pageable = PageRequest.of(page, size);

        Page<OrderItem> itemsPage = orderItemRepository.findBySellerIdWithDetails(seller.getId(), pageable);

        return itemsPage.map(this::toSellerOrderItemDto);
    }

    @Override
    public Page<OrderDTO> getOrderRequests(String email, int page, int size) {
        User seller = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found"));

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<Order> ordersPage = orderRepository.findBySellerIdAndStatusInOrderByCreatedAtDesc(
                seller.getId(), List.of(OrderStatus.PLACED), pageable);
        return ordersPage.map(this::toDto);
    }

    @Override
    public Page<OrderDTO> getOrderHistory(String email, boolean sellerSide, int page, int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<OrderStatus> historyStatuses = List.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));

        Page<Order> ordersPage;
        if (sellerSide) {
            ordersPage = orderRepository.findBySellerIdAndStatusInOrderByCreatedAtDesc(user.getId(), historyStatuses,
                    pageable);
        } else {
            ordersPage = orderRepository.findByCustomerIdAndStatusInOrderByCreatedAtDesc(user.getId(), historyStatuses,
                    pageable);
        }

        return ordersPage.map(this::toDto);
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
            if (product.getRentPrice() == null) {
                throw new IllegalArgumentException("Rental price not set for product: " + product.getTitle());
            }
            return product.getRentPrice();
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
        dto.setSellerId(order.getSeller().getId());
        dto.setSellerName(order.getSeller().getFullName());
        dto.setSellerEmail(order.getSeller().getEmail());
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
        dto.setProductImage(product.getPrimaryImageUrl());
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
            dto.setProductImageUrl(product.getPrimaryImageUrl());

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

    // ========== NEW METHODS FOR BUY FLOW ==========

    @Override
    public List<OrderDTO> getSellerOrders(String email) {
        User seller = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found"));

        List<Order> orders = orderRepository.findBySellerIdWithItemsAndProducts(seller.getId());
        return orders.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public OrderDTO approveOrder(Long orderId, String email) {
        User seller = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found"));

        Order order = orderRepository.findByIdWithItemsAndProducts(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Verify seller owns this order
        if (!order.getSeller().getId().equals(seller.getId())) {
            throw new IllegalStateException("You can only approve orders for your own products");
        }

        // Can only approve PLACED orders
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new IllegalStateException("Order can only be approved if it's in PLACED status");
        }

        // Get the first product from the order (assuming single product orders for now)
        Product product = order.getOrderItems().get(0).getProduct();
        int orderedQuantity = order.getOrderItems().get(0).getQuantity();

        // STEP 1 & 2: Fetch product with pessimistic lock and check quantity
        Product lockedProduct = productRepository.findByIdForUpdate(product.getId());
        if (lockedProduct == null) {
            throw new IllegalArgumentException("Product not found");
        }

        if (lockedProduct.getQuantity() == null || lockedProduct.getQuantity() <= 0) {
            // STEP 2: Reject this order if no quantity available
            order.setStatus(OrderStatus.REJECTED);
            orderRepository.save(order);
            throw new IllegalStateException("Product already sold");
        }

        // STEP 3: Reduce product quantity
        int newQuantity = lockedProduct.getQuantity() - orderedQuantity;
        lockedProduct.setQuantity(newQuantity);

        // STEP 4: If quantity becomes 0, set forSale = false
        if (newQuantity <= 0) {
            lockedProduct.setForSale(false);
        }

        // STEP 5: Save product and order
        productRepository.save(lockedProduct);
        order.setStatus(OrderStatus.APPROVED);
        Order saved = orderRepository.save(order);

        // STEP 6: Reject all other PLACED orders for the same product
        List<Order> otherPlacedOrders = orderRepository.findByProductIdAndStatusAndIdNot(
                product.getId(), OrderStatus.PLACED, orderId);

        for (Order otherOrder : otherPlacedOrders) {
            otherOrder.setStatus(OrderStatus.REJECTED);
            orderRepository.save(otherOrder);
        }

        // Create chat room (fixed to avoid duplicates)
        createChatRoomForOrder(saved);

        return toDto(saved);
    }

    private void createChatRoomForOrder(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }

        Long productId = order.getOrderItems().stream()
                .filter(item -> item.getProduct() != null)
                .map(item -> item.getProduct().getId())
                .findFirst().orElse(null);

        if (productId == null) {
            return;
        }

        Optional<ChatRoom> existingRoom = chatRoomRepository.findByProductIdAndBuyerIdAndSellerId(
                productId,
                order.getCustomer().getId(),
                order.getSeller().getId());

        if (existingRoom.isPresent()) {
            ChatRoom room = existingRoom.get();
            if (room.isDeleted()) {
                room.setDeleted(false);
            }
            if (room.getOrder() == null) {
                room.setOrder(order);
            }
            chatRoomRepository.saveAndFlush(room);
            return;
        }

        Optional<ChatRoom> deletedRoom = chatRoomRepository.findByProductIdAndBuyerIdAndSellerIdIncludeDeleted(
                productId,
                order.getCustomer().getId(),
                order.getSeller().getId());

        if (deletedRoom.isPresent()) {
            ChatRoom room = deletedRoom.get();
            room.setDeleted(false);
            if (room.getOrder() == null) {
                room.setOrder(order);
            }
            chatRoomRepository.saveAndFlush(room);
            return;
        }

        ChatRoom newRoom = ChatRoom.builder()
                .order(order)
                .productId(productId)
                .buyerId(order.getCustomer().getId())
                .sellerId(order.getSeller().getId())
                .build();

        chatRoomRepository.save(newRoom);
    }

    @Override
    @Transactional
    public OrderDTO rejectOrder(Long orderId, String email) {
        User seller = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found"));

        Order order = orderRepository.findByIdWithItemsAndProducts(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Verify seller owns this order
        if (!order.getSeller().getId().equals(seller.getId())) {
            throw new IllegalStateException("You can only reject orders for your own products");
        }

        // Can only reject PLACED orders
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new IllegalStateException("Order can only be rejected if it's in PLACED status");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        // If a product buy order is cancelled, restore stock and availability
        for (OrderItem item : order.getOrderItems()) {
            if (item.getType() == OrderItemType.BUY) {
                if (item.getProductVariant() != null) {
                    item.getProductVariant().increaseQuantity(item.getQuantity());
                } else {
                    Product product = item.getProduct();
                    Integer currentQty = product.getQuantity();
                    if (currentQty != null) {
                        product.setQuantity(currentQty + item.getQuantity());
                    }
                }
                item.getProduct().setForSale(true);
            }
        }

        return toDto(saved);
    }

    @Override
    public List<SellerOrderItemDTO> getSellerOrderItems(String email) {
        User seller = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found"));

        List<OrderItem> orderItems = orderItemRepository.findBySellerIdWithOrderAndProductDetails(seller.getId());
        return orderItems.stream().map(this::toSellerOrderItemDto).toList();
    }

    @Transactional
    @Override
    public SellerOrderItemDTO approveOrderItem(Long orderItemId, String email) {

        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        if (item.getProduct().getQuantity() <= 0) {
            throw new IllegalStateException("Product already sold");
        }

        item.getProduct().setQuantity(item.getProduct().getQuantity() - 1);

        // Optional: mark item approved (you can add status field later)

        orderItemRepository.save(item);

        return toSellerOrderItemDto(item);
    }

    @Override
    @Transactional
    public SellerOrderItemDTO rejectOrderItem(Long orderItemId, String email) {
        User seller = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found"));

        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("Order item not found"));

        // Verify seller owns this product
        if (!orderItem.getProduct().getSeller().getId().equals(seller.getId())) {
            throw new IllegalStateException("You can only reject order items for your own products");
        }

        // Can only reject PLACED orders
        if (orderItem.getOrder().getStatus() != OrderStatus.PLACED) {
            throw new IllegalStateException("Order item can only be rejected if the order is in PLACED status");
        }

        // Reject the entire order when any item is rejected
        orderItem.getOrder().setStatus(OrderStatus.REJECTED);

        // Restore stock and make product available again
        for (OrderItem item : orderItem.getOrder().getOrderItems()) {
            if (item.getType() == OrderItemType.BUY) {
                if (item.getProductVariant() != null) {
                    item.getProductVariant().increaseQuantity(item.getQuantity());
                } else {
                    Product product = item.getProduct();
                    Integer currentQty = product.getQuantity();
                    if (currentQty != null) {
                        product.setQuantity(currentQty + item.getQuantity());
                    }
                }
                item.getProduct().setForSale(true);
            }
        }

        orderRepository.save(orderItem.getOrder());

        return toSellerOrderItemDto(orderItem);
    }

    private SellerOrderItemDTO toSellerOrderItemDto(OrderItem orderItem) {
        SellerOrderItemDTO dto = new SellerOrderItemDTO();
        dto.setOrderItemId(orderItem.getId());
        dto.setOrderId(orderItem.getOrder().getId());
        dto.setProductId(orderItem.getProduct().getId());
        dto.setProductName(orderItem.getProduct().getTitle());
        dto.setProductBrand(orderItem.getProduct().getBrand());
        dto.setImage(orderItem.getProduct().getPrimaryImageUrl());
        dto.setQuantity(orderItem.getQuantity());
        dto.setPrice(orderItem.getTotalPrice());
        dto.setType(orderItem.getType().toString());
        dto.setOrderStatus(orderItem.getOrder().getStatus().toString());
        dto.setCreatedAt(orderItem.getCreatedAt());
        dto.setCustomerName(orderItem.getOrder().getCustomer().getFullName());
        dto.setCustomerEmail(orderItem.getOrder().getCustomer().getEmail());
        return dto;
    }

    @Override
    public Page<OrderDTO> getSellerOrders(String email, int page, int size) {
        User seller = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> ordersPage = orderRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId(), pageable);
        return ordersPage.map(this::toDto);
    }

    @Override
    @Transactional
    public OrderDTO cancelOrder(Long orderId, String email, String reason) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Order order = orderRepository.findByIdWithItemsAndProducts(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Check authorization - customer or seller can cancel
        boolean isCustomer = order.getCustomer().getId().equals(user.getId());
        boolean isSeller = order.getSeller().getId().equals(user.getId());

        if (!isCustomer && !isSeller) {
            throw new IllegalStateException("You can only cancel your own orders or orders for your products");
        }

        // Validate cancellation rules
        if (order.getStatus() == OrderStatus.CANCELLED_BY_CUSTOMER ||
                order.getStatus() == OrderStatus.CANCELLED_BY_SELLER) {
            throw new IllegalStateException("Order is already cancelled");
        }

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel order that is already delivered or completed");
        }

        // Update order status and cancellation details
        if (isCustomer) {
            order.setStatus(OrderStatus.CANCELLED_BY_CUSTOMER);
            order.setCancelledBy(Order.CancelledBy.CUSTOMER);
        } else {
            order.setStatus(OrderStatus.CANCELLED_BY_SELLER);
            order.setCancelledBy(Order.CancelledBy.SELLER);
        }

        order.setCancelReason(reason);
        order.setCancelledAt(java.time.LocalDateTime.now());

        // Restore stock for BUY items
        for (OrderItem item : order.getOrderItems()) {
            if (item.getType() == OrderItemType.BUY) {
                if (item.getProductVariant() != null) {
                    // Restore variant quantity
                    item.getProductVariant().increaseQuantity(item.getQuantity());
                } else {
                    // Restore product quantity (fallback)
                    Product product = item.getProduct();
                    Integer currentQty = product.getQuantity();
                    if (currentQty != null) {
                        product.setQuantity(currentQty + item.getQuantity());
                    }
                }
                // Make product available again
                item.getProduct().setForSale(true);
            }
        }

        Order saved = orderRepository.save(order);

        // Send notifications
        sendCancellationNotifications(saved, reason);

        return toDto(saved);
    }

    @Override
    @Transactional
    public SellerOrderItemDTO cancelOrderItem(Long orderItemId, String email, String reason) {
        User seller = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found"));

        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("Order item not found"));

        // Verify seller owns this product
        if (!orderItem.getProduct().getSeller().getId().equals(seller.getId())) {
            throw new IllegalStateException("You can only cancel order items for your own products");
        }

        Order order = orderItem.getOrder();

        // Validate cancellation rules
        if (order.getStatus() == OrderStatus.CANCELLED_BY_CUSTOMER ||
                order.getStatus() == OrderStatus.CANCELLED_BY_SELLER) {
            throw new IllegalStateException("Order is already cancelled");
        }

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel order that is already delivered or completed");
        }

        // Update order status and cancellation details
        order.setStatus(OrderStatus.CANCELLED_BY_SELLER);
        order.setCancelledBy(Order.CancelledBy.SELLER);
        order.setCancelReason(reason);
        order.setCancelledAt(java.time.LocalDateTime.now());

        // Restore stock for this specific item
        if (orderItem.getType() == OrderItemType.BUY) {
            if (orderItem.getProductVariant() != null) {
                // Restore variant quantity
                orderItem.getProductVariant().increaseQuantity(orderItem.getQuantity());
            } else {
                // Restore product quantity (fallback)
                Product product = orderItem.getProduct();
                Integer currentQty = product.getQuantity();
                if (currentQty != null) {
                    product.setQuantity(currentQty + orderItem.getQuantity());
                }
            }
        }

        orderRepository.save(order);

        // Send notifications
        sendCancellationNotifications(order, reason);

        return toSellerOrderItemDto(orderItem);
    }

    private void sendCancellationNotifications(Order order, String reason) {
        String productTitle = order.getOrderItems().isEmpty() ? "Product"
                : order.getOrderItems().get(0).getProduct().getTitle();

        if (order.getCancelledBy() == Order.CancelledBy.CUSTOMER) {
            // Notify seller
            emailService.sendOrderCancelledByCustomerEmail(
                    order.getSeller().getEmail(),
                    productTitle,
                    order.getCustomer().getFullName(),
                    order.getId().toString(),
                    reason);
        } else {
            // Notify customer
            emailService.sendOrderCancelledBySellerEmail(
                    order.getCustomer().getEmail(),
                    productTitle,
                    order.getSeller().getFullName(),
                    order.getId().toString(),
                    reason);
        }
    }
}
