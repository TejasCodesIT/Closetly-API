package com.closetly.closetly_backend.product.entity;

import com.closetly.closetly_backend.user.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Entity
@Table(name = "products")
// @Where(clause = "deleted = false")
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    private String brand;
    private String category;
    private String size;
    private String productCondition;

    @Enumerated(EnumType.STRING)
    private ProductType productType;

    private Double salePrice;
    private Double rentPricePerDay;
    private Double buyPrice;

    private Integer popularity = 0;

    @JsonProperty("isForSale")
    private boolean isForSale;
    @JsonProperty("isForRent")
    private boolean isForRent;

    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.ACTIVE;

    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "url")
    private List<String> images;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private boolean deleted = false;

    public boolean allowsRent() {
        if (productType == null) {
            return isForRent;
        }
        return productType == ProductType.RENT || productType == ProductType.BOTH;
    }

    public boolean allowsBuy() {
        if (productType == null) {
            return isForSale;
        }
        return productType == ProductType.BUY || productType == ProductType.BOTH;
    }

    public enum ProductStatus {
        ACTIVE,
        INACTIVE,
        BLOCKED
    }
}
