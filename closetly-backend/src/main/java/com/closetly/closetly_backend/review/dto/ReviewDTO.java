package com.closetly.closetly_backend.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewDTO {
    private Long id;

    @NotNull
    private Long productId;

    @NotNull
    private Long reviewerId;

    @Min(1)
    @Max(5)
    private Integer rating;

    private String comment;
}
