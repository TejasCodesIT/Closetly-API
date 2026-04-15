package com.closetly.closetly_backend.product.repository;

import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.entity.Product.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
        Page<Product> findByStatus(ProductStatus status, Pageable pageable);

        List<Product> findBySeller_IdOrderByCreatedAtDesc(Long sellerId);

        @Query(value = """
                        SELECT p.*,
                        (6371 * acos(
                            cos(radians(:lat))
                            * cos(radians(p.latitude))
                            * cos(radians(p.longitude) - radians(:lng))
                            + sin(radians(:lat))
                            * sin(radians(p.latitude))
                        )) AS distance
                        FROM products p
                        WHERE p.status = 'ACTIVE'
                        AND p.deleted = false
                        HAVING distance < :radius
                        ORDER BY distance ASC, p.created_at DESC
                        """, countQuery = """
                        SELECT COUNT(*) FROM (
                            SELECT p.*,
                            (6371 * acos(
                                cos(radians(:lat))
                                * cos(radians(p.latitude))
                                * cos(radians(p.longitude) - radians(:lng))
                                + sin(radians(:lat))
                                * sin(radians(p.latitude))
                            )) AS distance
                            FROM products p
                            WHERE p.status = 'ACTIVE'
                            AND p.deleted = false
                            HAVING distance < :radius
                        ) AS subquery
                        """, nativeQuery = true)
        Page<Product> findNearby(@Param("lat") double lat, @Param("lng") double lng, @Param("radius") double radius,
                        Pageable pageable);

        long countByCreatedAtAfter(LocalDateTime dateTime);

        // Find products available for sale (quantity > 0 and forSale = true)
        Page<Product> findByForSaleTrueAndQuantityGreaterThanAndDeletedFalse(int quantity, Pageable pageable);

        // Pessimistic lock for concurrent order approval
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT p FROM Product p WHERE p.id = :id")
        Product findByIdForUpdate(@Param("id") Long id);
}
