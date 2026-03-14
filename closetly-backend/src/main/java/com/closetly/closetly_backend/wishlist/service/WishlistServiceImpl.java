package com.closetly.closetly_backend.wishlist.service;

import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.repository.ProductRepository;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import com.closetly.closetly_backend.wishlist.dto.AddToWishlistRequestDTO;
import com.closetly.closetly_backend.wishlist.dto.WishlistDTO;
import com.closetly.closetly_backend.wishlist.entity.Wishlist;
import com.closetly.closetly_backend.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public WishlistDTO addToWishlist(AddToWishlistRequestDTO request, String userEmail) {
        // Verify authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !auth.getName().equals(userEmail)) {
            throw new AccessDeniedException("Not authenticated as the given user");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Check if already in wishlist
        if (wishlistRepository.existsByUserIdAndProductId(user.getId(), product.getId())) {
            throw new IllegalArgumentException("Product is already in wishlist");
        }

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();

        Wishlist saved = wishlistRepository.save(wishlist);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void removeFromWishlist(Long productId, String userEmail) {
        // Verify authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !auth.getName().equals(userEmail)) {
            throw new AccessDeniedException("Not authenticated as the given user");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Wishlist wishlist = wishlistRepository.findByUserIdAndProductId(user.getId(), productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found in wishlist"));

        // Soft delete
        wishlist.setDeleted(true);
        wishlistRepository.save(wishlist);
    }

    @Override
    public List<WishlistDTO> getUserWishlist(String userEmail) {
        // Verify authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !auth.getName().equals(userEmail)) {
            throw new AccessDeniedException("Not authenticated as the given user");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Wishlist> wishlists = wishlistRepository.findByUserIdWithProducts(user.getId());
        return wishlists.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public boolean isProductInWishlist(Long productId, String userEmail) {
        // Verify authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !auth.getName().equals(userEmail)) {
            throw new AccessDeniedException("Not authenticated as the given user");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return wishlistRepository.existsByUserIdAndProductId(user.getId(), productId);
    }

    private WishlistDTO toDto(Wishlist wishlist) {
        WishlistDTO dto = new WishlistDTO();
        dto.setId(wishlist.getId());
        dto.setUserId(wishlist.getUser().getId());
        dto.setProductId(wishlist.getProduct().getId());
        dto.setProductTitle(wishlist.getProduct().getTitle());
        dto.setProductDescription(wishlist.getProduct().getDescription());
        dto.setProductBrand(wishlist.getProduct().getBrand());
        dto.setProductSalePrice(wishlist.getProduct().getSalePrice());
        dto.setProductRentPricePerDay(wishlist.getProduct().getRentPricePerDay());
        dto.setProductImages(wishlist.getProduct().getImages());
        dto.setAddedAt(wishlist.getCreatedAt());
        return dto;
    }
}