package com.closetly.closetly_backend.cart.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AddToCartRequestDTO {
    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Type is required")
    private CartItemType type;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity = 1;

    private LocalDate startDate; // for rentals only
    private LocalDate endDate; // for rentals only

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