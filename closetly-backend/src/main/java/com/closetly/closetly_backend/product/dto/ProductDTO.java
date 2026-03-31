package com.closetly.closetly_backend.product.dto;

import com.closetly.closetly_backend.product.entity.ProductType;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    // Fixed: renamed to forSale/forRent to match Entity and Service layer
    // @JsonProperty maintains backward compatibility with API clients
    @JsonProperty("isForSale")
    private boolean forSale;

    @JsonProperty("isForRent")
    private boolean forRent;

    private Integer quantity;
    private Long sellerId;
    private Double latitude;
    private Double longitude;
    private String city;
    private String address;
    private String state;
    private Double distance;
    private List<String> images;
}
