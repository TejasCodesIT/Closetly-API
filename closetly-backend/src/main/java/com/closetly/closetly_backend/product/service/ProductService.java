package com.closetly.closetly_backend.product.service;

import com.closetly.closetly_backend.product.dto.ProductDTO;
import com.closetly.closetly_backend.product.dto.ProductRequestDTO;
import org.springframework.data.domain.Page;

public interface ProductService {
    ProductDTO createProduct(ProductRequestDTO request, String email);

    ProductDTO updateProduct(Long id, ProductRequestDTO request);

    void deleteProduct(Long id);

    ProductDTO getProductById(Long id);

    Page<ProductDTO> listActiveProducts(int page, int size);

    Page<ProductDTO> searchProducts(
            String query,
            String brand,
            String category,
            String size,
            String condition,
            Double minPrice,
            Double maxPrice,
            String type,
            String sort,
            int page,
            int sizePerPage);
}
