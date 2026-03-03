package com.closetly.closetly_backend.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ProductRequestDTO {
    @NotBlank
    private String title;

    private String description;
    private String brand;
    private String size;
    private String condition;

    private Double salePrice;
    private Double rentPricePerDay;

    private boolean isForSale;
    private boolean isForRent;

    @NotNull
    private Integer quantity;

    private Long sellerId; // id of the user listing this product

    private List<String> images;
}
