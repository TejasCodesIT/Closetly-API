package com.closetly.closetly_backend.product.service;

import com.closetly.closetly_backend.product.dto.ProductDTO;
import com.closetly.closetly_backend.product.dto.ProductRequestDTO;

import java.util.List;

public interface ProductService {
    ProductDTO createProduct(ProductRequestDTO request);

    ProductDTO updateProduct(Long id, ProductRequestDTO request);

    void deleteProduct(Long id);

    ProductDTO getProductById(Long id);

    List<ProductDTO> listActiveProducts();
}
