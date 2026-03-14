package com.closetly.closetly_backend.wishlist.controller;

import com.closetly.closetly_backend.wishlist.dto.AddToWishlistRequestDTO;
import com.closetly.closetly_backend.wishlist.dto.WishlistDTO;
import com.closetly.closetly_backend.wishlist.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/add")
    public ResponseEntity<WishlistDTO> addToWishlist(
            @Valid @RequestBody AddToWishlistRequestDTO request,
            Authentication authentication) {

        String email = authentication.getName();
        WishlistDTO result = wishlistService.addToWishlist(request, email);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<Void> removeFromWishlist(
            @PathVariable Long productId,
            Authentication authentication) {

        String email = authentication.getName();
        wishlistService.removeFromWishlist(productId, email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<WishlistDTO>> getUserWishlist(Authentication authentication) {
        String email = authentication.getName();
        List<WishlistDTO> wishlist = wishlistService.getUserWishlist(email);
        return ResponseEntity.ok(wishlist);
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<Map<String, Boolean>> checkProductInWishlist(
            @PathVariable Long productId,
            Authentication authentication) {

        String email = authentication.getName();
        boolean isInWishlist = wishlistService.isProductInWishlist(productId, email);
        return ResponseEntity.ok(Map.of("inWishlist", isInWishlist));
    }
}