package com.closetly.closetly_backend.cart.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CartItemDTO {
    private String id;
    private Long productId;
    private String productTitle;
    private String productBrand;
    private List<String> productImages;
    private Double productSalePrice;
    private Double productRentPricePerDay;
    private CartItemType type;
    private Integer quantity;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime addedAt;

    public enum CartItemType {
        RENT, BUY;

        @JsonCreator
        public static CartItemType fromString(String value) {
            return CartItemType.valueOf(value.toUpperCase());
        }

        @JsonValue
        public String toValue() {
            return this.name().toLowerCase();
        }
    }
}