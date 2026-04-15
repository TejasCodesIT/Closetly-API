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
    private Double rentPrice;
    private Double buyPrice;

    private Integer popularity = 0;

    // FIX: Rename fields to avoid Lombok/Jackson issues with "is" prefix boolean
    // fields
    // Changed: isForSale -> forSale, isForRent -> forRent
    // @JsonProperty("isForSale") is used to maintain backward compatibility with
    // API
    @JsonProperty("isForSale")
    @Column(name = "is_for_sale")
    private boolean forSale;

    @JsonProperty("isForRent")
    @Column(name = "is_for_rent")
    private boolean forRent;

    private Integer quantity;

    private Double latitude;

    private Double longitude;

    private String address;
    private String city;

    private String state;

    @Transient
    private Double distance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.ACTIVE;

    @ElementCollection(fetch = FetchType.EAGER)
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
            return forRent;
        }
        return productType == ProductType.RENT || productType == ProductType.BOTH;
    }

    public boolean allowsBuy() {
        if (productType == null) {
            return forSale;
        }
        return productType == ProductType.BUY || productType == ProductType.BOTH;
    }

    public enum ProductStatus {
        ACTIVE,
        INACTIVE,
        BLOCKED
    }
}
