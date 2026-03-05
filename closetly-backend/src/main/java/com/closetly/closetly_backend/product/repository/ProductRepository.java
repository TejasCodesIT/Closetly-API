package com.closetly.closetly_backend.product.repository;

import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.entity.Product.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    // add more as needed for filtering
}
