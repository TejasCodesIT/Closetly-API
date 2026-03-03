package com.closetly.closetly_backend.product.service;

import com.closetly.closetly_backend.product.dto.ProductDTO;
import com.closetly.closetly_backend.product.dto.ProductRequestDTO;
import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.entity.Product.ProductStatus;
import com.closetly.closetly_backend.product.repository.ProductRepository;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
    public ProductDTO createProduct(ProductRequestDTO request) {
        // seller must exist
        User seller = userRepository.findById(request.getSellerId())
                .orElseThrow(() -> new IllegalArgumentException("Seller not found: " + request.getSellerId()));

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
                .status(ProductStatus.ACTIVE)
                .build();

        Product saved = productRepository.save(product);
        return toDto(saved);
    }

    @Override
    public ProductDTO updateProduct(Long id, ProductRequestDTO request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

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
        if (request.getSellerId() != null && !request.getSellerId().equals(existing.getSeller().getId())) {
            User seller = userRepository.findById(request.getSellerId())
                    .orElseThrow(() -> new IllegalArgumentException("Seller not found: " + request.getSellerId()));
            existing.setSeller(seller);
        }

        Product updated = productRepository.save(existing);
        return toDto(updated);
    }

    @Override
    public void deleteProduct(Long id) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
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
    public List<ProductDTO> listActiveProducts() {
        return productRepository.findByStatus(ProductStatus.ACTIVE).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
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
