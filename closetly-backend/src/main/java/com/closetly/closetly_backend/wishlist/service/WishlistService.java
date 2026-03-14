package com.closetly.closetly_backend.wishlist.service;

import com.closetly.closetly_backend.wishlist.dto.AddToWishlistRequestDTO;
import com.closetly.closetly_backend.wishlist.dto.WishlistDTO;

import java.util.List;

public interface WishlistService {
    WishlistDTO addToWishlist(AddToWishlistRequestDTO request, String userEmail);

    void removeFromWishlist(Long productId, String userEmail);

    List<WishlistDTO> getUserWishlist(String userEmail);

    boolean isProductInWishlist(Long productId, String userEmail);
}