package com.closetly.closetly_backend.product.repository;

import com.closetly.closetly_backend.product.entity.Product;
import com.closetly.closetly_backend.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    Optional<ProductVariant> findByProductIdAndSize(Long productId, String size);

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.product.id = :productId AND pv.size = :size")
    Optional<ProductVariant> findByProductAndSize(@Param("productId") Long productId, @Param("size") String size);

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.product = :product AND pv.size = :size")
    Optional<ProductVariant> findByProductAndSize(@Param("product") Product product, @Param("size") String size);
}