package com.closetly.closetly_backend.review.service;

import com.closetly.closetly_backend.review.dto.ReviewDTO;

import java.util.List;

public interface ReviewService {
    ReviewDTO createReview(ReviewDTO reviewDTO);

    List<ReviewDTO> getReviewsForProduct(Long productId);
}
