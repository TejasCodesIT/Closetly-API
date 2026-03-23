package com.closetly.closetly_backend.cart.service;

import com.closetly.closetly_backend.cart.dto.AddToCartRequestDTO;
import com.closetly.closetly_backend.cart.dto.CartItemDTO;
import com.closetly.closetly_backend.cart.dto.CartSummaryDTO;

import java.util.List;

public interface CartService {
    CartItemDTO addToCart(AddToCartRequestDTO request, String userEmail);

    void removeFromCart(String cartItemId, String userEmail);

    List<CartItemDTO> getCartItems(String userEmail);

    CartSummaryDTO getCartSummary(String userEmail);

    CartItemDTO updateQuantity(String cartItemId, Integer quantity, String userEmail);

    void clearCart(String userEmail);
}