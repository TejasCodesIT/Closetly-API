package com.closetly.closetly_backend.product.service;

import com.closetly.closetly_backend.product.dto.ProductDTO;
import com.closetly.closetly_backend.product.dto.ProductRequestDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {
        ProductDTO createProduct(ProductRequestDTO request, String email);

        ProductDTO updateProduct(Long id, ProductRequestDTO request);

        void deleteProduct(Long id);

        ProductDTO getProductById(Long id);

        Page<ProductDTO> listActiveProducts(int page, int size);

        List<ProductDTO> getMyProducts(String email);

        Page<ProductDTO> searchProducts(
                        String query,
                        String brand,
                        String category,
                        String itemSize, // ✅ Renamed from 'size'
                        String condition,
                        Double minPrice,
                        Double maxPrice,
                        String type,
                        String sort,
                        int page,
                        int sizePerPage);

        Page<ProductDTO> findNearbyProducts(double lat, double lng, double radiusKm, int page, int size, String sort);

        /**
         * Search products with support for location + filters combined
         * All location parameters are optional - if null, location filtering is skipped
         */
        Page<ProductDTO> searchProductsWithLocation(
                        String query,
                        String brand,
                        String category,
                        String itemSize, // ✅ Renamed from 'size'
                        String condition,
                        Double minPrice,
                        Double maxPrice,
                        String type,
                        String sort,
                        Double latitude,
                        Double longitude,
                        Double radiusKm,
                        int page,
                        int sizePerPage);
}
