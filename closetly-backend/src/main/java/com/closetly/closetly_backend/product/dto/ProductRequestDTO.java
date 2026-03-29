package com.closetly.closetly_backend.product.dto;

import com.closetly.closetly_backend.product.entity.ProductType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Slf4j
public class ProductRequestDTO {
    @NotBlank
    private String title;

    private String description;
    private String brand;
    private String category;
    private String size;
    private String condition;

    private Double salePrice;
    private Double rentPricePerDay;
    private Double buyPrice;

    private ProductType productType;

    // ✅ FIXED: Jackson requires explicit @JsonProperty for is* boolean fields
    // Without this, Jackson cannot properly deserialize boolean fields starting
    // with "is"
    // because Lombok generates setForSale() but Jackson expects setIsForSale()
    @JsonProperty("isForSale")
    private boolean isForSale;

    @JsonProperty("isForRent")
    private boolean isForRent;

    @NotNull
    private Integer quantity;

    @NotNull(message = "Product location is required")
    private Double latitude;

    @NotNull(message = "Product location is required")
    private Double longitude;

    private String city;

    private Long sellerId; // id of the user listing this product

    private List<String> images;

    public void validate() {
        log.debug("[ProductRequestDTO Deserialization] RECEIVED VALUES:");
        log.debug("[ProductRequestDTO Deserialization]   isForSale: {} (type: boolean)", isForSale);
        log.debug("[ProductRequestDTO Deserialization]   isForRent: {} (type: boolean)", isForRent);
        log.debug("[ProductRequestDTO Deserialization]   productType: {}", productType);
        log.debug("[ProductRequestDTO Deserialization]   salePrice: {}", salePrice);
        log.debug("[ProductRequestDTO Deserialization]   rentPricePerDay: {}", rentPricePerDay);
        log.debug("[ProductRequestDTO Deserialization]   description: '{}' (length: {})",
                description, description != null ? description.length() : 0);
        log.debug("[ProductRequestDTO Deserialization]   images count: {}",
                images != null ? images.size() : 0);

        log.debug("[ProductRequestDTO Validation] Starting validation");
        log.debug("[ProductRequestDTO Validation] isForSale: {}, isForRent: {}", isForSale, isForRent);

        if (!isForSale && !isForRent) {
            log.error("[ProductRequestDTO Validation] Both isForSale and isForRent are false");
            throw new IllegalArgumentException("Product must be available for sale, rent, or both");
        }

        if (isForSale && (salePrice == null || salePrice <= 0)) {
            log.error("[ProductRequestDTO Validation] isForSale is true but salePrice is invalid: {}", salePrice);
            throw new IllegalArgumentException("salePrice is required and must be positive when isForSale is true");
        }

        if (isForRent && (rentPricePerDay == null || rentPricePerDay <= 0)) {
            log.error("[ProductRequestDTO Validation] isForRent is true but rentPricePerDay is invalid: {}",
                    rentPricePerDay);
            throw new IllegalArgumentException(
                    "rentPricePerDay is required and must be positive when isForRent is true");
        }

        if (latitude == null || longitude == null) {
            log.error("[ProductRequestDTO Validation] Missing latitude/longitude");
            throw new IllegalArgumentException("Product location is required");
        }

        if (images == null || images.isEmpty()) {
            log.error("[ProductRequestDTO Validation] No images provided");
            throw new IllegalArgumentException("At least one image is required");
        }

        if (images.size() > 10) {
            log.error("[ProductRequestDTO Validation] Too many images: {}", images.size());
            throw new IllegalArgumentException("Maximum 10 images are allowed");
        }

        log.debug(
                "[ProductRequestDTO Validation] All validations passed - images: {}, salePrice: {}, rentPricePerDay: {}",
                images.size(), salePrice, rentPricePerDay);
    }
}
