package com.closetly.closetly_backend.product.repository;

import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.entity.Product.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    List<Product> findBySeller_IdOrderByCreatedAtDesc(Long sellerId);

    @Query(value = "SELECT p.* FROM products p " +
            "WHERE p.status = 'ACTIVE' AND p.deleted = false " +
            "AND (6371 * acos(cos(radians(:lat)) * cos(radians(p.latitude)) * cos(radians(p.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(p.latitude)))) < :radius "
            +
            "ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(p.latitude)) * cos(radians(p.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(p.latitude)))) ASC", nativeQuery = true)
    List<Product> findNearby(@Param("lat") double lat, @Param("lng") double lng, @Param("radius") double radius);

    long countByCreatedAtAfter(LocalDateTime dateTime);
}
