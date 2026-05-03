package com.closetly.closetly_backend.wishlist.dto;

import com.closetly.closetly_backend.product.entity.ProductType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WishlistDTO {
    private Long id;
    private Long userId;
    private Long productId;
    private String productTitle;
    private String productDescription;
    private String productBrand;
    private Double productSalePrice;
    private Double productRentPricePerDay;
    private List<String> productImages;
    private ProductType productType;
    private boolean isForSale;
    private boolean isForRent;
    private LocalDateTime addedAt;
}