package com.closetly.closetly_backend.cart.service;

import com.closetly.closetly_backend.cart.dto.AddToCartRequestDTO;
import com.closetly.closetly_backend.cart.dto.CartItemDTO;
import com.closetly.closetly_backend.cart.dto.CartSummaryDTO;
import com.closetly.closetly_backend.product.dto.ProductDTO;
import com.closetly.closetly_backend.cart.entity.CartItem;
import com.closetly.closetly_backend.cart.repository.CartItemRepository;
import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.repository.ProductRepository;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public CartItemDTO addToCart(AddToCartRequestDTO request, String userEmail) {
        System.out.println("DEBUG: addToCart called with request: " + request);
        System.out.println("DEBUG: userEmail: " + userEmail);
        System.out.println("DEBUG: request.type: " + (request.getType() != null ? request.getType() : "NULL"));

        // Verify authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !auth.getName().equals(userEmail)) {
            System.out.println("DEBUG: Authentication failed. auth: " + auth + ", userEmail: " + userEmail);
            throw new AccessDeniedException("Not authenticated as the given user");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Validate product availability based on type
        if (request.getType() == null) {
            throw new IllegalArgumentException("Cart item type is required");
        }
        validateProductForCart(product, request.getType());

        // Validate rental dates if it's a rental
        if (request.getType() == AddToCartRequestDTO.CartItemType.RENT) {
            validateRentalDates(request.getStartDate(), request.getEndDate());
        }

        // Check if item already exists in cart (same product, same type)
        CartItem.CartItemType cartItemType = CartItem.CartItemType.valueOf(request.getType().name());
        System.out.println("DEBUG: cartItemType: " + cartItemType);
        boolean exists = cartItemRepository.existsByUserIdAndProductIdAndType(
                user.getId(), product.getId(), cartItemType);

        if (exists) {
            throw new IllegalArgumentException("Product is already in cart with the same type");
        }

        // Create cart item
        CartItem cartItem = CartItem.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .product(product)
                .type(cartItemType)
                .quantity(request.getQuantity())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        CartItem saved = cartItemRepository.save(cartItem);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void removeFromCart(String cartItemId, String userEmail) {
        // Verify authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !auth.getName().equals(userEmail)) {
            throw new AccessDeniedException("Not authenticated as the given user");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CartItem cartItem = cartItemRepository.findByIdAndUserId(cartItemId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        cartItemRepository.delete(cartItem);
    }

    @Override
    public List<CartItemDTO> getCartItems(String userEmail) {
        // Verify authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !auth.getName().equals(userEmail)) {
            throw new AccessDeniedException("Not authenticated as the given user");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<CartItem> cartItems = cartItemRepository.findByUserIdWithProducts(user.getId());
        return cartItems.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public CartSummaryDTO getCartSummary(String userEmail) {
        List<CartItemDTO> items = getCartItems(userEmail);

        CartSummaryDTO summary = new CartSummaryDTO();
        summary.setItems(items);
        summary.setTotalItems(items.size());

        double totalAmount = 0.0;
        double rentalTotal = 0.0;
        double purchaseTotal = 0.0;

        for (CartItemDTO item : items) {
            double itemTotal = calculateItemTotal(item);
            totalAmount += itemTotal;

            if (item.getType() == CartItemDTO.CartItemType.RENT) {
                rentalTotal += itemTotal;
            } else {
                purchaseTotal += itemTotal;
            }
        }

        summary.setTotalAmount(totalAmount);
        summary.setRentalItemsTotal(rentalTotal);
        summary.setPurchaseItemsTotal(purchaseTotal);

        return summary;
    }

    @Override
    @Transactional
    public CartItemDTO updateQuantity(String cartItemId, Integer quantity, String userEmail) {
        // Verify authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !auth.getName().equals(userEmail)) {
            throw new AccessDeniedException("Not authenticated as the given user");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CartItem cartItem = cartItemRepository.findByIdAndUserId(cartItemId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        cartItem.setQuantity(quantity);
        CartItem saved = cartItemRepository.save(cartItem);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void clearCart(String userEmail) {
        // Verify authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !auth.getName().equals(userEmail)) {
            throw new AccessDeniedException("Not authenticated as the given user");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<CartItem> items = cartItemRepository.findByUserId(user.getId());
        cartItemRepository.deleteAll(items);
    }

    private void validateProductForCart(Product product, AddToCartRequestDTO.CartItemType type) {
        if (type == AddToCartRequestDTO.CartItemType.RENT && !product.allowsRent()) {
            throw new IllegalArgumentException("Product is not available for rent");
        }
        if (type == AddToCartRequestDTO.CartItemType.BUY && !product.allowsBuy()) {
            throw new IllegalArgumentException("Product is not available for purchase");
        }
        if (product.getStatus() != Product.ProductStatus.ACTIVE) {
            throw new IllegalArgumentException("Product is not available");
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

    private double calculateItemTotal(CartItemDTO item) {
        if (item.getType() == CartItemDTO.CartItemType.RENT && item.getProduct().getRentPrice() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(item.getStartDate(), item.getEndDate()) + 1;
            return item.getProduct().getRentPrice() * days * item.getQuantity();
        } else if (item.getType() == CartItemDTO.CartItemType.BUY && item.getProduct().getSalePrice() != null) {
            return item.getProduct().getSalePrice() * item.getQuantity();
        }
        return 0.0;
    }

    private CartItemDTO toDto(CartItem cartItem) {
        ProductDTO productDto = new ProductDTO();
        Product product = cartItem.getProduct();
        productDto.setId(product.getId());
        productDto.setTitle(product.getTitle());
        productDto.setDescription(product.getDescription());
        productDto.setBrand(product.getBrand());
        productDto.setCategory(product.getCategory());
        productDto.setSize(product.getSize());
        productDto.setCondition(product.getProductCondition());
        productDto.setProductType(product.getProductType());
        productDto.setSalePrice(product.getSalePrice());
        productDto.setRentPrice(product.getRentPrice());
        productDto.setBuyPrice(product.getBuyPrice());
        productDto.setPopularity(product.getPopularity());
        productDto.setForSale(product.allowsBuy());
        productDto.setForRent(product.allowsRent());
        productDto.setQuantity(product.getQuantity());
        productDto.setSellerId(product.getSeller() != null ? product.getSeller().getId() : null);
        productDto.setImages(product.getImages());

        CartItemDTO dto = new CartItemDTO();
        dto.setId(cartItem.getId());
        dto.setProduct(productDto);
        dto.setType(CartItemDTO.CartItemType.valueOf(cartItem.getType().name()));
        dto.setQuantity(cartItem.getQuantity());
        dto.setStartDate(cartItem.getStartDate());
        dto.setEndDate(cartItem.getEndDate());
        dto.setAddedAt(cartItem.getCreatedAt());
        return dto;
    }
}