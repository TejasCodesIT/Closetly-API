package com.closetly.closetly_backend.cart.dto;

import lombok.Data;

import java.util.List;

@Data
public class CartSummaryDTO {
    private List<CartItemDTO> items;
    private int totalItems;
    private double totalAmount;
    private double rentalItemsTotal;
    private double purchaseItemsTotal;
}