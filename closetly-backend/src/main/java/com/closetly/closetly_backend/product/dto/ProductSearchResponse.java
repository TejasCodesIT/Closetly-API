package com.closetly.closetly_backend.product.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class ProductSearchResponse {
    private List<ProductDTO> products;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;

    public static ProductSearchResponse fromPage(Page<ProductDTO> page) {
        ProductSearchResponse response = new ProductSearchResponse();
        response.setProducts(page.getContent());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setCurrentPage(page.getNumber());
        response.setPageSize(page.getSize());
        return response;
    }
}
