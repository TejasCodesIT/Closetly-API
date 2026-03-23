package com.closetly.closetly_backend.cart.controller;

import com.closetly.closetly_backend.cart.dto.AddToCartRequestDTO;
import com.closetly.closetly_backend.cart.dto.CartItemDTO;
import com.closetly.closetly_backend.cart.dto.UpdateQuantityRequestDTO;
import com.closetly.closetly_backend.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<List<CartItemDTO>> getCartItems(Authentication authentication) {
        String email = authentication.getName();
        List<CartItemDTO> cartItems = cartService.getCartItems(email);
        return ResponseEntity.ok(cartItems);
    }

    @PostMapping
    public ResponseEntity<CartItemDTO> addToCart(
            @Valid @RequestBody AddToCartRequestDTO request,
            Authentication authentication) {

        String email = authentication.getName();
        CartItemDTO result = cartService.addToCart(request, email);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<Void> removeFromCart(
            @PathVariable String cartItemId,
            Authentication authentication) {

        String email = authentication.getName();
        cartService.removeFromCart(cartItemId, email);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{cartItemId}/quantity")
    public ResponseEntity<CartItemDTO> updateQuantity(
            @PathVariable String cartItemId,
            @Valid @RequestBody UpdateQuantityRequestDTO request,
            Authentication authentication) {

        String email = authentication.getName();
        CartItemDTO result = cartService.updateQuantity(cartItemId, request.getQuantity(), email);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        String email = authentication.getName();
        cartService.clearCart(email);
        return ResponseEntity.noContent().build();
    }
}