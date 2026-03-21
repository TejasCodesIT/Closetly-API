package com.closetly.closetly_backend.admin.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String category;
    private String condition;
    private Long sellerId;
    private String sellerName;
    private String status;
    private Boolean approved;
    private LocalDateTime createdAt;
    private Integer viewCount;
    private Integer bookingCount;
    private List<String> images;
}