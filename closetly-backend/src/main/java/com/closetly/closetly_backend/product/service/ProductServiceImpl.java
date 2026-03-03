package com.closetly.closetly_backend.product.service;

import com.closetly.closetly_backend.product.dto.ProductDTO;
import com.closetly.closetly_backend.product.dto.ProductRequestDTO;
import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.entity.Product.ProductStatus;
import com.closetly.closetly_backend.product.repository.ProductRepository;
import com.closetly.closetly_backend.user.entity.Role;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public ProductDTO createProduct(ProductRequestDTO request, String email) {
        // verify authenticated user matches email and has USER role
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !auth.getName().equals(email)) {
            throw new AccessDeniedException("Not authenticated as the given seller");
        }

        User seller = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Seller not found"));

        boolean isUserRole = seller.getRoles().stream()
            .map(Role::getName)
            .anyMatch(r -> r == Role.RoleName.USER);
        if (!isUserRole) {
            throw new AccessDeniedException("Only users with USER role can create products");
        }

        Product product = Product.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .brand(request.getBrand())
            .size(request.getSize())
            .productCondition(request.getCondition())
            .salePrice(request.getSalePrice())
            .rentPricePerDay(request.getRentPricePerDay())
            .isForSale(request.isForSale())
            .isForRent(request.isForRent())
            .quantity(request.getQuantity())
            .images(request.getImages())
            .seller(seller)
            .status(Product.ProductStatus.ACTIVE)
            .build();

        Product saved = productRepository.save(product);

        return toDto(saved);
    }

    @Override
    public ProductDTO updateProduct(Long id, ProductRequestDTO request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        // ownership check - only seller can update
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        User authUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        if (!existing.getSeller().getId().equals(authUser.getId())) {
            throw new AccessDeniedException("Only the seller can update this product");
        }

        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setBrand(request.getBrand());
        existing.setSize(request.getSize());
        existing.setProductCondition(request.getCondition());
        existing.setSalePrice(request.getSalePrice());
        existing.setRentPricePerDay(request.getRentPricePerDay());
        existing.setForSale(request.isForSale());
        existing.setForRent(request.isForRent());
        existing.setQuantity(request.getQuantity());
        existing.setImages(request.getImages());
        // do not allow changing seller via update

        Product updated = productRepository.save(existing);
        return toDto(updated);
    }

    @Override
    public void deleteProduct(Long id) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        // only seller may delete (soft delete)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        User authUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        if (!existing.getSeller().getId().equals(authUser.getId())) {
            throw new AccessDeniedException("Only the seller can delete this product");
        }
        existing.setDeleted(true);
        productRepository.save(existing);
    }

    @Override
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        return toDto(product);
    }

    
    @Override
    public Page<ProductDTO> listActiveProducts(int page, int size) {
        var pg = productRepository.findByStatus(ProductStatus.ACTIVE, PageRequest.of(page, size));
        List<ProductDTO> content = pg.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return new PageImpl<>(content, pg.getPageable(), pg.getTotalElements());
    }

    private ProductDTO toDto(Product p) {
        ProductDTO dto = new ProductDTO();
        dto.setId(p.getId());
        dto.setTitle(p.getTitle());
        dto.setDescription(p.getDescription());
        dto.setBrand(p.getBrand());
        dto.setSize(p.getSize());
        dto.setCondition(p.getProductCondition());
        dto.setSalePrice(p.getSalePrice());
        dto.setRentPricePerDay(p.getRentPricePerDay());
        dto.setForSale(p.isForSale());
        dto.setForRent(p.isForRent());
        dto.setQuantity(p.getQuantity());
        dto.setSellerId(p.getSeller() != null ? p.getSeller().getId() : null);
        dto.setImages(p.getImages());
        return dto;
    }
}
