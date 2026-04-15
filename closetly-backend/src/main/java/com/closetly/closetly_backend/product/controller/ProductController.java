package com.closetly.closetly_backend.product.controller;

import com.closetly.closetly_backend.product.dto.ProductDTO;
import com.closetly.closetly_backend.product.dto.ProductRequestDTO;
import com.closetly.closetly_backend.product.dto.ProductSearchResponse;
import com.closetly.closetly_backend.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {
    private final ProductService productService;

    @GetMapping("/mine")
    public ResponseEntity<List<ProductDTO>> mine(Authentication authentication) {
        return ResponseEntity.ok(productService.getMyProducts(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<ProductDTO> create(
            @Valid @RequestBody ProductRequestDTO request,
            Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(productService.createProduct(request, email));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO request,
            Authentication authentication) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<ProductSearchResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String itemSize, // ✅ Renamed from 'size' to avoid conflict
            @RequestParam(required = false, name = "condition") String condition,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radius,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name="size", defaultValue = "20") int sizePerPage) {

        // ✅ DEBUG LOGGING
        log.info("[API] /search endpoint called");
        log.info("[API] Filter params: query={}, brand={}, category={}, itemSize={}, condition={}",
                query, brand, category, itemSize, condition);
        log.info("[API] Price params: minPrice={}, maxPrice={}", minPrice, maxPrice);
        log.info("[API] Sort: {}, Type: {}", sort, type);
        log.info("[API] Location params: latitude={}, longitude={}, radius={}", latitude, longitude, radius);
        log.info("[API] Pagination: page={}, size={}", page, sizePerPage);

        // If location parameters are provided, use unified search with location
        if (latitude != null && longitude != null && radius != null) {
            log.info("[API] Using UNIFIED search with location filter");
            var results = productService.searchProductsWithLocation(
                    query,
                    brand,
                    category,
                    itemSize, // ✅ Updated parameter name
                    condition,
                    minPrice,
                    maxPrice,
                    type,
                    sort,
                    latitude,
                    longitude,
                    radius,
                    page,
                    sizePerPage);
            log.info("[API] Search returned {} results", results.getTotalElements());
            return ResponseEntity.ok(ProductSearchResponse.fromPage(results));
        }

        // Otherwise, use regular search without location
        log.info("[API] Using REGULAR search without location");
        var results = productService.searchProducts(
                query,
                brand,
                category,
                itemSize, // ✅ Updated parameter name
                condition,
                minPrice,
                maxPrice,
                type,
                sort,
                page,
                sizePerPage);

        log.info("[API] Search returned {} results", results.getTotalElements());
        return ResponseEntity.ok(ProductSearchResponse.fromPage(results));
    }

    @GetMapping("/nearby")
    public ResponseEntity<ProductSearchResponse> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "20") double radius,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "distance") String sort) {

        log.info("[API] /nearby endpoint - lat={}, lng={}, radius={}, page={}, size={}, sort={}",
                lat, lng, radius, page, size, sort);

        var results = productService.findNearbyProducts(lat, lng, radius, page, size, sort);

        log.info("[API] Nearby search returned {} results", results.getSize());
        return ResponseEntity.ok(ProductSearchResponse.fromPage(results));
    }

    @GetMapping
    public ResponseEntity<Page<ProductDTO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.listActiveProducts(page, size));
    }
}
