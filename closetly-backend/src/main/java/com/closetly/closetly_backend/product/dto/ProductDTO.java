package com.closetly.closetly_backend.product.dto;

import com.closetly.closetly_backend.product.entity.ProductType;
import lombok.Data;

import java.util.List;

@Data
public class ProductDTO {
    private Long id;
    private String title;
    private String description;
    private String brand;
    private String category;
    private String size;
    private String condition;
    private ProductType productType;
    private Double salePrice;
    private Double rentPricePerDay;
    private Double buyPrice;
    private Integer popularity;
    private boolean isForSale;
    private boolean isForRent;
    private Integer quantity;
    private Long sellerId;
    private List<String> images;
}
